"""Soft-delete only the active sessions captured in a verified, user-scoped export."""
import argparse
import hashlib
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from urllib.request import Request, urlopen
from api_auth import request_headers


def request(base, path, method="GET"):
    with urlopen(Request(base + "/api/v1" + path, method=method, headers=request_headers(base)), timeout=30) as response:
        body = response.read()
        return json.loads(body) if body else None


def atomic_json(path, value):
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    os.replace(temporary, path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("backup", type=Path)
    parser.add_argument("--api-url", default="http://127.0.0.1:8080")
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    base = args.api_url.rstrip("/")
    manifest = json.loads((args.backup / "manifest.json").read_text(encoding="utf-8"))
    verification = json.loads((args.backup / "restore-verification.json").read_text(encoding="utf-8"))
    if verification.get("result") != "PASS" or verification.get("sqlSha256") != manifest["sha256"]["history-inserts.sql"]:
        raise SystemExit("An exact, verified SQL restore is required before archiving.")
    for name, expected in manifest["sha256"].items():
        if hashlib.sha256((args.backup / name).read_bytes()).hexdigest() != expected:
            raise SystemExit(f"Backup checksum mismatch: {name}")
    replay = json.loads((args.backup / "replay-inputs.json").read_text(encoding="utf-8"))
    identity = request(base, "/identity/me")
    if identity["userId"] != manifest["userId"] or replay["userId"] != manifest["userId"]:
        raise SystemExit("API identity does not match the backup user.")
    target_hash = hashlib.sha256(base.encode()).hexdigest()[:12]
    journal_path = args.backup / ("archive-receipts-" + target_hash + ".json")
    journal = json.loads(journal_path.read_text(encoding="utf-8")) if journal_path.exists() else {
        "userId": identity["userId"], "apiUrl": base, "replaySha256": manifest["sha256"]["replay-inputs.json"], "receipts": []}
    if journal["apiUrl"] != base or journal["userId"] != identity["userId"] or journal["replaySha256"] != manifest["sha256"]["replay-inputs.json"]:
        raise SystemExit("Archive journal belongs to another target or export.")
    done = {item["sourceSessionId"] for item in journal["receipts"]}
    pending = [item for item in replay["sessions"] if item["sourceSessionId"] not in done]
    raw = json.loads((args.backup / "raw-snapshot.json").read_text(encoding="utf-8"))
    old_entries = {item["id"]: sorted((e for e in raw["tables"]["entries"] if e["session_id"] == item["id"]), key=lambda e: e["entry_order"])
                   for item in raw["tables"]["sessions"]}
    # Check every target before the first mutation; version conflicts during deletion still stop safely.
    for item in pending:
        details = request(base, "/training/sessions/" + item["sourceSessionId"])
        command = item["command"]
        if details["version"] != item["sourceVersion"] or details["sessionDate"] != command["sessionDate"] or details["areaId"] != command["areaId"]:
            raise SystemExit("A session changed since export. Create a fresh backup before deleting.")
        expected = old_entries[item["sourceSessionId"]]
        if len(expected) != len(details["entries"]):
            raise SystemExit("Entry count changed since export.")
        field_map = {"clientEntryId": "client_entry_id", "entryOrder": "entry_order", "resultType": "result_type",
                     "climbId": "climb_id", "gradeLabel": "grade_label", "effortProfileCode": "effort_profile_code",
                     "ascentMode": "ascent_mode", "protectionMode": "protection_mode", "totalMoves": "total_moves",
                     "executedMoves": "executed_moves", "notes": "notes"}
        for old, entry in zip(expected, sorted(details["entries"], key=lambda e: e["entryOrder"])):
            if any(old[column] != entry.get(field) for field, column in field_map.items()):
                raise SystemExit("Session inputs changed since export; no deletion performed.")
    if not args.apply:
        print(json.dumps({"checkedSessions": len(pending), "userId": identity["userId"], "action": "VALIDATION ONLY; add --apply to soft-delete"}))
        return
    atomic_json(journal_path, journal)
    for item in pending:
        journal["pendingSourceSessionId"] = item["sourceSessionId"]
        atomic_json(journal_path, journal)
        request(base, f"/training/sessions/{item['sourceSessionId']}?expectedVersion={item['sourceVersion']}", "DELETE")
        journal["receipts"].append({"sourceSessionId": item["sourceSessionId"], "sourceVersion": item["sourceVersion"], "archivedAt": datetime.now(timezone.utc).isoformat()})
        journal.pop("pendingSourceSessionId", None)
        atomic_json(journal_path, journal)
    print(json.dumps({"archivedSessions": len(journal["receipts"]), "userId": identity["userId"], "apiUrl": base}))


if __name__ == "__main__":
    main()
