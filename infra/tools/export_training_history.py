"""Export one local user's exact SQL history and input-only commands for later recalculation."""
import argparse
import hashlib
import json
import subprocess
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path


TABLES = {
    "sessions": "training.training_sessions",
    "entries": "training.training_entries",
    "revisions": "training.training_session_revisions",
}


def exact_json(value):
    if isinstance(value, Decimal):
        return str(value)
    if isinstance(value, dict):
        return "{" + ",".join(json.dumps(key, ensure_ascii=False) + ":" + exact_json(item) for key, item in value.items()) + "}"
    if isinstance(value, list):
        return "[" + ",".join(exact_json(item) for item in value) + "]"
    return json.dumps(value, ensure_ascii=False)


def psql(container, database, sql):
    result = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-X", "-qAt", "-v", "ON_ERROR_STOP=1",
         "-U", "climbbetter", "-d", database],
        input=sql.encode("utf-8"), capture_output=True, check=True,
    )
    return result.stdout.decode("utf-8").strip()


def snapshot_query(user_id):
    return f"""
BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;
WITH sessions AS (
 SELECT * FROM training.training_sessions WHERE user_id = '{user_id}'::uuid
), entries AS (
 SELECT e.* FROM training.training_entries e JOIN sessions s ON s.id=e.session_id
), revisions AS (
 SELECT r.* FROM training.training_session_revisions r JOIN sessions s ON s.id=r.session_id
), active AS (
 SELECT e.* FROM entries e JOIN sessions s ON s.id=e.session_id WHERE s.deleted_at IS NULL
)
SELECT json_build_object(
 'user', (SELECT row_to_json(u) FROM identity.users u WHERE id='{user_id}'::uuid),
 'schemaVersion', (SELECT version FROM public.flyway_schema_history WHERE success ORDER BY installed_rank DESC LIMIT 1),
 'tables', json_build_object(
   'sessions', COALESCE((SELECT json_agg(s ORDER BY session_date, started_at, created_at, id) FROM sessions s),'[]'::json),
   'entries', COALESCE((SELECT json_agg(e ORDER BY session_id, entry_order, id) FROM entries e),'[]'::json),
   'revisions', COALESCE((SELECT json_agg(r ORDER BY session_id, revision_number, id) FROM revisions r),'[]'::json)
 ),
 'columns', (SELECT json_object_agg(table_name, cols) FROM (
   SELECT table_name, json_agg(column_name ORDER BY ordinal_position) cols FROM information_schema.columns
   WHERE table_schema='training' AND table_name IN ('training_sessions','training_entries','training_session_revisions')
   GROUP BY table_name) c),
 'integrity', json_build_object(
   'sessions', (SELECT md5(COALESCE(string_agg(row_to_json(s)::text, ',' ORDER BY id),'')) FROM sessions s),
   'entries', (SELECT md5(COALESCE(string_agg(row_to_json(e)::text, ',' ORDER BY id),'')) FROM entries e),
   'revisions', (SELECT md5(COALESCE(string_agg(row_to_json(r)::text, ',' ORDER BY id),'')) FROM revisions r)
 ),
 'preflight', json_build_object(
   'invalidCompleted', (SELECT count(*) FROM active WHERE result_type='ASCENT' AND executed_moves<>total_moves),
   'invalidMoves', (SELECT count(*) FROM active WHERE executed_moves<=0 OR (result_type<>'WARMUP' AND (total_moves IS NULL OR total_moves<=0))),
   'inactiveProfiles', (SELECT count(*) FROM active e LEFT JOIN training.effort_profiles p ON p.code=e.effort_profile_code AND p.is_active WHERE e.result_type<>'WARMUP' AND p.id IS NULL),
   'inactiveGrades', (SELECT count(*) FROM active e LEFT JOIN catalog.grades g ON g.scale_code=e.grade_scale_code AND g.label=e.grade_label AND g.is_active WHERE e.result_type<>'WARMUP' AND g.id IS NULL),
   'unavailableClimbs', (SELECT count(*) FROM active e LEFT JOIN catalog.climbs c ON c.id=e.climb_id WHERE e.result_type<>'WARMUP' AND (c.id IS NULL OR c.is_archived OR (NOT c.is_public AND c.owner_id<>'{user_id}'::uuid))),
   'unavailableAreas', (SELECT count(*) FROM sessions s JOIN catalog.areas a ON a.id=s.area_id WHERE s.deleted_at IS NULL AND (a.is_archived OR a.resolution_status='MERGED' OR (NOT a.is_public AND a.owner_id<>'{user_id}'::uuid))),
   'changedClimbNames', (SELECT count(*) FROM active e JOIN catalog.climbs c ON c.id=e.climb_id WHERE c.name<>e.climb_name_snapshot),
   'changedAreaNames', (SELECT count(*) FROM sessions s JOIN catalog.areas a ON a.id=s.area_id WHERE s.deleted_at IS NULL AND a.name<>s.location_name_snapshot)
 ),
 'preserved', json_build_object(
   'users', (SELECT md5(COALESCE(string_agg(row_to_json(t)::text, ',' ORDER BY id),'')) FROM identity.users t),
   'areas', (SELECT md5(COALESCE(string_agg(row_to_json(t)::text, ',' ORDER BY id),'')) FROM catalog.areas t),
   'climbs', (SELECT md5(COALESCE(string_agg(row_to_json(t)::text, ',' ORDER BY id),'')) FROM catalog.climbs t),
   'otherSessions', (SELECT md5(COALESCE(string_agg(row_to_json(t)::text, ',' ORDER BY id),'')) FROM training.training_sessions t WHERE user_id<>'{user_id}'::uuid),
   'otherEntries', (SELECT md5(COALESCE(string_agg(row_to_json(e)::text, ',' ORDER BY e.id),'')) FROM training.training_entries e JOIN training.training_sessions s ON s.id=e.session_id WHERE s.user_id<>'{user_id}'::uuid),
   'otherRevisions', (SELECT md5(COALESCE(string_agg(row_to_json(r)::text, ',' ORDER BY r.id),'')) FROM training.training_session_revisions r JOIN training.training_sessions s ON s.id=r.session_id WHERE s.user_id<>'{user_id}'::uuid)
 )
);
COMMIT;
"""


def replay_inputs(snapshot, user_id, exported_at):
    result = []
    entries = snapshot["tables"]["entries"]
    for session in snapshot["tables"]["sessions"]:
        if session["deleted_at"] is not None:
            continue
        commands, confirmed = [], []
        for source in sorted((e for e in entries if e["session_id"] == session["id"]), key=lambda e: (e["entry_order"], e["id"])):
            entry = {"clientEntryId": str(uuid.uuid4()), "entryOrder": source["entry_order"],
                     "resultType": source["result_type"], "executedMoves": source["executed_moves"], "notes": source["notes"]}
            if source["result_type"] != "WARMUP":
                for target, column in {"climbId": "climb_id", "climbName": "climb_name_snapshot", "climbType": "climb_type",
                                       "gradeLabel": "grade_label", "effortProfileCode": "effort_profile_code", "ascentMode": "ascent_mode",
                                       "protectionMode": "protection_mode", "totalMoves": "total_moves"}.items():
                    entry[target] = source[column]
                entry["historyConflictConfirmed"] = False
                if source["history_conflict_confirmed"]:
                    confirmed.append(entry["clientEntryId"])
            commands.append(entry)
        result.append({
            "sourceSessionId": session["id"], "sourceClientSessionId": session["client_session_id"], "sourceVersion": session["version"],
            "confirmedHistoryEntryIds": confirmed,
            "command": {"clientSessionId": str(uuid.uuid4()), "sessionName": session["session_name"],
                        "sessionDate": session["session_date"], "timeZoneId": session["time_zone_id"],
                        "startedAt": session["started_at"], "endedAt": session["ended_at"], "areaId": session["area_id"], "entries": commands},
        })
    return {"formatVersion": 1, "userId": user_id, "exportedAt": exported_at, "sessions": result}


def restore_sql(snapshot, user_id):
    lines = [
        "-- Exact history restoration, NOT recalculation. Requires the original catalog and compatible schema.",
        f"-- Replaces ONLY training history for user {user_id}, including deleted sessions and audit.",
        "-- Do not execute after a replay unless you intend to replace that user's new history.",
        "BEGIN;", "SET standard_conforming_strings = on;",
        f"DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM identity.users WHERE id='{user_id}'::uuid) THEN RAISE EXCEPTION 'Expected backup user does not exist'; END IF; END $$;",
        f"DELETE FROM training.training_session_revisions r USING training.training_sessions s WHERE r.session_id=s.id AND s.user_id='{user_id}'::uuid;",
        f"DELETE FROM training.training_entries e USING training.training_sessions s WHERE e.session_id=s.id AND s.user_id='{user_id}'::uuid;",
        f"DELETE FROM training.training_sessions WHERE user_id='{user_id}'::uuid;",
    ]
    for key, table in TABLES.items():
        columns = ", ".join('"' + col + '"' for col in snapshot["columns"][table.split(".")[1]])
        for row in snapshot["tables"][key]:
            payload = exact_json(row).replace("'", "''")
            lines.append(f"INSERT INTO {table} ({columns}) SELECT {columns} FROM json_populate_record(NULL::{table}, '{payload}'::json);")
    return "\n".join([*lines, "COMMIT;", ""])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--user-id", required=True, type=uuid.UUID)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--container", default="climbbetter-postgres")
    parser.add_argument("--database", default="climbbetter")
    args = parser.parse_args()
    if args.output.exists():
        raise SystemExit("Refusing to overwrite an existing backup directory.")
    user_id = str(args.user_id)
    raw = psql(args.container, args.database, snapshot_query(user_id))
    snapshot = json.loads(raw, parse_float=Decimal)
    if not snapshot["user"]:
        raise SystemExit("User does not exist; no backup created.")
    exported_at = datetime.now(timezone.utc).isoformat()
    replay = replay_inputs(snapshot, user_id, exported_at)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output / "raw-snapshot.json").write_text(raw + "\n", encoding="utf-8")
    (args.output / "history-inserts.sql").write_text(restore_sql(snapshot, user_id), encoding="utf-8")
    (args.output / "replay-inputs.json").write_text(exact_json(replay) + "\n", encoding="utf-8")
    files = {name: hashlib.sha256((args.output / name).read_bytes()).hexdigest()
             for name in ["raw-snapshot.json", "history-inserts.sql", "replay-inputs.json"]}
    manifest = {"formatVersion": 1, "userId": user_id, "exportedAt": exported_at,
                "schemaVersion": snapshot["schemaVersion"], "allSessions": len(snapshot["tables"]["sessions"]),
                "activeSessions": len(replay["sessions"]), "allEntries": len(snapshot["tables"]["entries"]),
                "activeEntries": sum(len(s["command"]["entries"]) for s in replay["sessions"]),
                "revisions": len(snapshot["tables"]["revisions"]), "preflight": snapshot["preflight"],
                "integrity": snapshot["integrity"], "preserved": snapshot["preserved"], "sha256": files}
    (args.output / "manifest.json").write_text(exact_json(manifest) + "\n", encoding="utf-8")
    print(json.dumps({key: manifest[key] for key in ["userId", "allSessions", "activeSessions", "allEntries", "activeEntries", "revisions", "preflight"]}))
    if any(snapshot["preflight"].values()):
        raise SystemExit("Backup completed, but replay preflight needs review. Do not archive sessions yet.")


if __name__ == "__main__":
    main()
