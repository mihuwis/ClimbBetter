"""Verify a user's INSERT backup in an empty database with only scoped dependencies."""
import argparse
import hashlib
import json
import subprocess
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path

from export_training_history import exact_json, psql, snapshot_query


def docker(container, *arguments):
    return subprocess.run(["docker", "exec", container, *arguments], capture_output=True, check=True)


def dependencies_query(user_id):
    # Only catalog rows referenced by this user's history, plus non-personal calculation dictionaries.
    return f"""
WITH s AS (SELECT * FROM training.training_sessions WHERE user_id='{user_id}'::uuid),
 e AS (SELECT e.* FROM training.training_entries e JOIN s ON s.id=e.session_id),
 c AS (SELECT c.* FROM catalog.climbs c WHERE c.id IN (SELECT climb_id FROM e)),
 a AS (SELECT a.* FROM catalog.areas a WHERE a.id IN (SELECT area_id FROM s UNION SELECT area_id FROM c)),
 sec AS (SELECT sec.* FROM catalog.sectors sec WHERE sec.id IN (SELECT sector_id FROM c))
SELECT json_build_object(
 'catalog.areas', COALESCE((SELECT json_agg(a) FROM a),'[]'::json),
 'catalog.sectors', COALESCE((SELECT json_agg(sec) FROM sec),'[]'::json),
 'catalog.climbs', COALESCE((SELECT json_agg(c) FROM c),'[]'::json),
 'catalog.grades', (SELECT json_agg(g) FROM catalog.grades g),
 'training.effort_profiles', (SELECT json_agg(p) FROM training.effort_profiles p),
 'training.style_rules', (SELECT json_agg(r) FROM training.style_rules r),
 'training.relative_effort_rules', (SELECT json_agg(r) FROM training.relative_effort_rules r)
);
"""


def insert_rows(table, rows):
    statements = []
    for row in rows:
        columns = ", ".join('"' + name + '"' for name in row)
        payload = exact_json(row).replace("'", "''")
        statements.append(f"INSERT INTO {table} ({columns}) SELECT {columns} FROM json_populate_record(NULL::{table}, '{payload}'::json);")
    return "\n".join(statements)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backup", type=Path)
    parser.add_argument("--container", default="climbbetter-postgres")
    parser.add_argument("--database", default="climbbetter")
    parser.add_argument("--keep-test-database", action="store_true", help="Keep the scoped scratch database for a separate API replay check")
    args = parser.parse_args()
    manifest = json.loads((args.backup / "manifest.json").read_text(encoding="utf-8"))
    for name, expected in manifest["sha256"].items():
        if hashlib.sha256((args.backup / name).read_bytes()).hexdigest() != expected:
            raise SystemExit(f"Backup checksum mismatch: {name}")
    user_id = str(uuid.UUID(manifest["userId"]))
    live = json.loads(psql(args.container, args.database, snapshot_query(user_id)))
    if live["integrity"] != manifest["integrity"] or live["preserved"] != manifest["preserved"]:
        raise SystemExit("History or catalog changed since export; create a fresh backup first.")
    scoped = json.loads(psql(args.container, args.database, dependencies_query(user_id)), parse_float=Decimal)
    # Copy no other user's identity row. Public-reference owners get synthetic FK placeholders only.
    owners = {row["owner_id"] for table in ["catalog.areas", "catalog.climbs"] for row in scoped[table] if row.get("owner_id")}
    actors = {row["changed_by_user_id"] for row in live["tables"]["revisions"]}
    placeholders = [{"id": identifier, "display_name": "Reference placeholder", "created_at": "2000-01-01T00:00:00Z"}
                    for identifier in sorted((owners | actors) - {user_id})]
    scoped = {"identity.users": [live["user"], *placeholders], **scoped}
    order = ["identity.users", "catalog.grades", "training.effort_profiles", "training.style_rules",
             "training.relative_effort_rules", "catalog.areas", "catalog.sectors", "catalog.climbs"]
    dependencies_sql = "BEGIN;\nSET standard_conforming_strings=on;\n" + "\n".join(insert_rows(table, scoped[table]) for table in order) + "\nCOMMIT;\n"
    schema = docker(args.container, "pg_dump", "-U", "climbbetter", "--schema-only", "--no-owner", "--no-privileges",
                    "--schema=identity", "--schema=catalog", "--schema=training", "--schema=reporting", args.database).stdout.decode("utf-8")
    (args.backup / "schema-only.sql").write_text(schema, encoding="utf-8")
    (args.backup / "scoped-dependencies.sql").write_text(dependencies_sql, encoding="utf-8")
    temporary_db = "cb_history_verify_" + uuid.uuid4().hex[:16]
    created, passed = False, False
    query_without_flyway = snapshot_query(user_id).replace(
        "(SELECT version FROM public.flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1)", "'schema-only'")
    try:
        docker(args.container, "createdb", "-U", "climbbetter", temporary_db)
        created = True
        psql(args.container, temporary_db, schema)
        psql(args.container, temporary_db, dependencies_sql)
        before = json.loads(psql(args.container, temporary_db, query_without_flyway))
        psql(args.container, temporary_db, (args.backup / "history-inserts.sql").read_text(encoding="utf-8"))
        restored = json.loads(psql(args.container, temporary_db, query_without_flyway))
        if restored["integrity"] != manifest["integrity"] or restored["preserved"] != before["preserved"]:
            raise SystemExit("Restored history does not match the exported records or affected dependency data.")
        counts = {key: len(rows) for key, rows in restored["tables"].items()}
        if counts != {"sessions": manifest["allSessions"], "entries": manifest["allEntries"], "revisions": manifest["revisions"]}:
            raise SystemExit("Restored record counts do not match the manifest.")
        report = {"verifiedAt": datetime.now(timezone.utc).isoformat(), "userId": user_id,
                  "verifiedInDisposableDatabase": temporary_db, "restoredCounts": counts,
                  "integrity": restored["integrity"], "dependencyDataUnchanged": True,
                  "sqlSha256": manifest["sha256"]["history-inserts.sql"],
                  "scope": "User history, directly referenced catalog rows, shared calculation dictionaries, schema only; no other users' identity/history copied",
                  "result": "PASS"}
        (args.backup / "restore-verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        passed = True
        print(json.dumps({"result": "PASS", "restoredCounts": counts, "testDatabase": temporary_db, "kept": args.keep_test_database}))
    finally:
        if created and (not args.keep_test_database or not passed):
            docker(args.container, "dropdb", "-U", "climbbetter", temporary_db)


if __name__ == "__main__":
    main()
