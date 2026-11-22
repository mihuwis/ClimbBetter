"""Validate a history export, or explicitly replay its inputs through the current API.

Default: offline validation only. --apply verifies the current user and makes POSTs.
Keep the replay-state file: it preserves pending requests and prevents duplicate imports.
"""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import tempfile
from datetime import date, datetime, timezone
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen
from api_auth import request_headers
from uuid import UUID


SESSION_FIELDS = set("clientSessionId sessionName sessionDate timeZoneId startedAt endedAt areaId clientAreaId areaName entries".split())
ENTRY_FIELDS = set("clientEntryId entryOrder resultType climbId clientClimbId climbName climbType gradeLabel effortProfileCode ascentMode protectionMode totalMoves executedMoves notes historyConflictConfirmed".split())


def require(condition, message):
    if not condition:
        raise ValueError(message)


def uuid(value):
    require(isinstance(value, str), "UUID must be a string")
    return str(UUID(value))


def instant(value):
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(parsed.tzinfo is not None, "Session timestamps must include an offset")
    return parsed


def validate_export(data):
    require(isinstance(data, dict) and type(data.get("formatVersion")) is int and data["formatVersion"] == 1, "Unsupported export format")
    user_id = uuid(data.get("userId"))
    sessions = data.get("sessions")
    require(isinstance(sessions, list) and sessions and all(isinstance(item, dict) for item in sessions), "The export has no valid sessions")
    sources = [uuid(item.get("sourceSessionId")) for item in sessions]
    require(len(set(sources)) == len(sources), "Repeated sourceSessionId")
    client_ids, entry_ids = set(), set()
    previous_date = None
    for item in sessions:
        require(type(item.get("sourceVersion")) is int and item["sourceVersion"] >= 0, "Invalid source version")
        command = item.get("command")
        require(isinstance(command, dict) and not set(command) - SESSION_FIELDS, "Unexpected session fields; export inputs only")
        client_id = uuid(command.get("clientSessionId"))
        require(client_id not in sources and client_id not in client_ids, "New clientSessionId must be unique and different from source IDs")
        if item.get("sourceClientSessionId") is not None:
            require(client_id != uuid(item["sourceClientSessionId"]), "Replay must use a new clientSessionId; the original remains reserved after soft deletion")
        client_ids.add(client_id)
        session_date = date.fromisoformat(command["sessionDate"])
        require(previous_date is None or session_date >= previous_date, "Sessions must be in chronological order")
        previous_date = session_date
        require(command.get("timeZoneId") and instant(command["endedAt"]) > instant(command["startedAt"]), "Invalid session times")
        require(bool(command.get("areaId")) != bool(command.get("clientAreaId")), "Choose areaId or clientAreaId")
        uuid(command.get("areaId") or command.get("clientAreaId"))
        entries = command.get("entries")
        require(isinstance(entries, list) and entries, "A session has no entries")
        confirmed = {uuid(value) for value in item.get("confirmedHistoryEntryIds", [])}
        local_ids = set()
        for index, entry in enumerate(entries):
            require(isinstance(entry, dict) and not set(entry) - ENTRY_FIELDS, "Unexpected entry fields; calculations/history cannot be replayed")
            entry_id = uuid(entry.get("clientEntryId"))
            require(entry_id not in entry_ids, "clientEntryId must be unique across the export")
            entry_ids.add(entry_id)
            local_ids.add(entry_id)
            require(type(entry.get("entryOrder")) is int and entry["entryOrder"] == index, "Entry order must be contiguous from zero")
            require(type(entry.get("executedMoves")) is int and entry["executedMoves"] > 0, "Executed moves must be positive whole numbers")
            require(entry.get("historyConflictConfirmed") is None or entry["historyConflictConfirmed"] is False, "Initial replay commands must not pre-confirm history conflicts")
            result = entry.get("resultType")
            require(result in ("ASCENT", "ATTEMPT", "WARMUP"), "Invalid result type")
            if result == "WARMUP":
                require(not any(entry.get(key) is not None for key in ENTRY_FIELDS - {"clientEntryId", "entryOrder", "resultType", "executedMoves", "notes", "historyConflictConfirmed"}), "Warm-up contains climbing fields")
                continue
            require(bool(entry.get("climbId")) != bool(entry.get("clientClimbId")), "Choose climbId or clientClimbId")
            uuid(entry.get("climbId") or entry.get("clientClimbId"))
            require(entry.get("climbType") in ("BOULDER", "ROUTE", "CIRCUIT"), "Invalid climb type")
            require(entry.get("gradeLabel") and entry.get("effortProfileCode"), "Missing grade or effort profile")
            require(entry.get("ascentMode") in ("OS", "FLASH", "RP"), "Invalid ascent mode")
            protection = entry.get("protectionMode")
            require(protection in ("LEAD", "TOP_ROPE") if entry["climbType"] == "ROUTE" else protection is None, "Invalid protection mode")
            require(type(entry.get("totalMoves")) is int and entry["totalMoves"] > 0, "Total moves must be positive whole numbers")
            require(result != "ASCENT" or entry["executedMoves"] == entry["totalMoves"], f"Completed entry {entry_id} has mismatched moves")
        require(confirmed <= local_ids, "Confirmation allowlist contains an unknown entry ID")
    return user_id, sessions


def save_json(path, value):
    temporary = None
    try:
        with tempfile.NamedTemporaryFile("w", encoding="utf-8", dir=path.parent, prefix=f".{path.name}.", suffix=".tmp", delete=False) as output:
            temporary = Path(output.name)
            json.dump(value, output, ensure_ascii=False, indent=2)
            output.write("\n")
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if temporary and temporary.exists():
            temporary.unlink()


def api_request(api_url, path, payload=None):
    body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = Request(api_url + "/api/v1" + path, data=body, headers={"Accept": "application/json", "Content-Type": "application/json", **request_headers(api_url)})
    try:
        with urlopen(request, timeout=60) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        try:
            details = json.loads(error.read().decode("utf-8"))
        except (ValueError, UnicodeError):
            details = {"detail": str(error)}
        return error.code, details


def validate_pending(original, payload, allowed):
    normalized = copy.deepcopy(payload)
    require(isinstance(normalized, dict) and isinstance(normalized.get("entries"), list), "Invalid pending payload")
    require(len(normalized["entries"]) == len(original["entries"]), "Pending entry count changed")
    for entry, source in zip(normalized["entries"], original["entries"]):
        if entry.get("historyConflictConfirmed") is True:
            require(entry.get("clientEntryId") in allowed, "Pending payload confirms an unauthorized history conflict")
            if "historyConflictConfirmed" in source:
                entry["historyConflictConfirmed"] = source["historyConflictConfirmed"]
            else:
                entry.pop("historyConflictConfirmed")
    require(normalized == original, "Pending payload differs from exported inputs")


def replay(data, raw, input_path, api_url):
    # The OS releases this advisory lock even after an interrupted process.
    with input_path.with_suffix(".replay-state.lock").open("a+b") as lock:
        if not os.fstat(lock.fileno()).st_size:
            lock.write(b"0")
            lock.flush()
        lock.seek(0)
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(lock.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as error:
            raise ValueError("Another importer is using this replay journal") from error
        _replay(data, raw, input_path, api_url)


def _replay(data, raw, input_path, api_url):
    user_id, sessions = validate_export(data)
    status, identity = api_request(api_url, "/identity/me")
    require(status == 200 and uuid(identity.get("userId")) == user_id, "API identity does not match the exported user")
    state_path = input_path.with_suffix(".replay-state.json")
    header = {"formatVersion": 1, "inputSha256": hashlib.sha256(raw).hexdigest(), "apiUrl": api_url, "userId": user_id}
    state = json.loads(state_path.read_text(encoding="utf-8")) if state_path.exists() else {**header, "sessions": {}}
    require(all(state.get(key) == value for key, value in header.items()), "Replay journal belongs to another input, API target or user")
    require(isinstance(state.get("sessions"), dict), "Invalid replay journal")
    require(set(state["sessions"]) <= {item["sourceSessionId"] for item in sessions}, "Journal has unknown source sessions")
    # New client IDs make duplicate imports possible, so verify the old history is absent first.
    needs_post = any(state["sessions"].get(item["sourceSessionId"], {}).get("status") != "complete" for item in sessions)
    if needs_post:
        for item in sessions:
            status, _ = api_request(api_url, "/training/sessions/" + item["sourceSessionId"])
            require(status == 404, f"Source session {item['sourceSessionId']} must be archived before replay (GET returned HTTP {status}); no sessions imported")
    for index, item in enumerate(sessions, 1):
        source_id = item["sourceSessionId"]
        record = state["sessions"].get(source_id)
        allowed = set(item.get("confirmedHistoryEntryIds", []))
        if record:
            validate_pending(item["command"], record.get("payload"), allowed)
            if record.get("status") == "complete":
                uuid(record.get("sessionId"))
                print(f"{index}/{len(sessions)} already imported: {record['sessionId']}")
                continue
            require(record.get("status") == "pending", "Invalid journal status")
        else:
            record = {"status": "pending", "payload": copy.deepcopy(item["command"])}
            state["sessions"][source_id] = record
        while True:
            save_json(state_path, state)  # Persist the exact request before every potentially mutating call.
            status, response = api_request(api_url, "/training/sessions", record["payload"])
            if status in (200, 201):
                record.update(status="complete", sessionId=uuid(response.get("sessionId")), importedAt=datetime.now(timezone.utc).isoformat())
                save_json(state_path, state)
                print(f"{index}/{len(sessions)} imported: {record['sessionId']}")
                break
            entry_id = response.get("clientEntryId")
            if status == 409 and response.get("code") == "ASCENT_MODE_HISTORY_CONFLICT" and entry_id in allowed:
                entry = next((entry for entry in record["payload"]["entries"] if entry["clientEntryId"] == entry_id), None)
                if entry and entry.get("historyConflictConfirmed") is not True:
                    entry["historyConflictConfirmed"] = True
                    continue
            raise ValueError(f"Import stopped at source {source_id}: HTTP {status}: {response.get('detail', response)}. Pending request kept in {state_path}")
    print(f"Import complete. Journal: {state_path}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("replay_file", type=Path)
    parser.add_argument("--api-url", default="http://127.0.0.1:8080", help="Backend base URL, without /api/v1")
    parser.add_argument("--apply", action="store_true", help="Explicitly import through the API; default validates offline only")
    args = parser.parse_args()
    try:
        raw = args.replay_file.read_bytes()
        data = json.loads(raw.decode("utf-8-sig"))
        user_id, sessions = validate_export(data)
        print(f"Validated {len(sessions)} sessions, {sum(len(item['command']['entries']) for item in sessions)} entries for {user_id}.")
        if not args.apply:
            print("Validation only: no API requests or changes. Use --apply after the new calculator is ready.")
            return
        api_url = args.api_url.rstrip("/")
        parts = urlsplit(api_url)
        require(parts.scheme in ("http", "https") and parts.netloc and not parts.query and not parts.fragment, "Invalid API base URL")
        replay(data, raw, args.replay_file.resolve(), api_url)
    except (ValueError, KeyError, TypeError, OSError, URLError) as error:
        parser.exit(1, f"Error: {error}\n")


if __name__ == "__main__":
    main()
