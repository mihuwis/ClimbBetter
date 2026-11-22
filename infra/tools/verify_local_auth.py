"""Exercise the Auth Emulator -> Java -> separate model database and seed reproducible example training."""
import argparse
import base64
import json
import uuid
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlencode, urlparse
from urllib.request import Request, urlopen


def call(base, path, method='GET', body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    request = Request(base + path, method=method, headers=headers,
        data=None if body is None else json.dumps(body).encode())
    try:
        with urlopen(request, timeout=20) as response:
            raw = response.read()
            return response.status, json.loads(raw) if raw else None
    except HTTPError as error:
        return error.code, None


def identifier(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'climbbetter:model:' + name))


def sign_in(subject, name):
    status, response = call('http://127.0.0.1:9099',
        '/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=local-emulator', 'POST', {
            'postBody': urlencode({'providerId': 'google.com', 'id_token': json.dumps({
                'sub': subject, 'email': subject + '@example.test', 'email_verified': True, 'name': name})}),
            'requestUri': 'http://localhost', 'returnSecureToken': True})
    assert status == 200, 'Emulator sign-in failed'
    return response['idToken'], response['localId']


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api-url', default='http://127.0.0.1:18081')
    parser.add_argument('--report', type=Path, default=Path('.local-backups/firebase-auth-implementation/emulator-api-verification.json'))
    args = parser.parse_args()
    api_url = args.api_url.rstrip('/')
    target = urlparse(api_url)
    if target.scheme != 'http' or target.hostname not in ('localhost', '127.0.0.1'):
        parser.error('Use a local backend in emulator mode with the separate model database')
    first, first_uid = sign_in('test-good-climber-1', 'Test_dobry_wspinacz_1')
    second, second_uid = sign_in('test-beginner-1', 'Test_poczatkujacy_1')
    api = lambda path, method='GET', body=None, token=first: call(api_url, '/api/v1' + path, method, body, token)
    assert api('/identity/me', token=None)[0] == 401
    assert api('/identity/me', token='broken')[0] == 401
    profiles = []
    for token, name in [(first, 'Test_dobry_wspinacz_1'), (second, 'Test_poczatkujacy_1')]:
        status, profile = api('/identity/bootstrap', 'POST', token=token)
        assert status == 200, 'Admin SDK did not accept the local emulator token'
        assert api('/identity/bootstrap', 'POST', token=token)[1]['userId'] == profile['userId']
        status, profile = api('/identity/me', 'PUT', {'displayName': name}, token)
        assert status == 200 and profile['profileComplete']
        profiles.append(profile)
    assert profiles[0]['userId'] != profiles[1]['userId']
    sessions = []
    for day in ['2026-11-01', '2026-11-08']:
        body = {'clientSessionId': identifier(day), 'sessionName': 'Model boulder progression ' + day,
            'sessionDate': day, 'timeZoneId': 'Europe/Warsaw', 'startedAt': day + 'T10:00:00Z',
            'endedAt': day + 'T11:30:00Z', 'clientAreaId': identifier('gym'), 'areaName': 'Model climbing gym',
            'entries': [{'clientEntryId': identifier(day + ':warmup'), 'entryOrder': 0, 'resultType': 'WARMUP', 'executedMoves': 40}]}
        for index, grade in enumerate(['6B', '6C', '7A'], 1):
            body['entries'].append({'clientEntryId': identifier(day + ':' + grade), 'entryOrder': index,
                'clientClimbId': identifier(grade), 'climbName': 'Model boulder ' + grade, 'climbType': 'BOULDER',
                'gradeLabel': grade, 'effortProfileCode': 'BOULDER', 'resultType': 'ASCENT', 'ascentMode': 'RP',
                'totalMoves': 8, 'executedMoves': 8})
        status, created = api('/training/sessions', 'POST', body)
        assert status == 201, 'Model training could not be created'
        session_id = created['sessionId']
        assert api('/training/sessions/' + session_id)[0] == 200
        assert api('/training/sessions/' + session_id, token=second)[0] == 404
        assert api('/training/sessions/' + session_id + '?expectedVersion=0', 'DELETE', token=second)[0] == 404
        sessions.append(session_id)
    assert api('/dashboard/sessions', token=second)[1] == []
    # Even in emulator mode, the real Admin SDK must reject a different issuer/audience.
    parts = first.split('.')
    claims = json.loads(base64.urlsafe_b64decode(parts[1] + '=' * (-len(parts[1]) % 4)))
    claims['aud'] = 'different-project'
    claims['iss'] = 'https://securetoken.google.com/different-project'
    parts[1] = base64.urlsafe_b64encode(json.dumps(claims).encode()).decode().rstrip('=')
    assert api('/identity/me', token='.'.join(parts))[0] == 401
    update = '/identitytoolkit.googleapis.com/v1/projects/demo-climbbetter/accounts:update'
    assert call('http://127.0.0.1:9099', update, 'POST', {'localId': second_uid, 'disableUser': True}, 'owner')[0] == 200
    try:
        assert api('/identity/me', token=second)[0] == 401, 'Disabled users must be rejected'
    finally:
        call('http://127.0.0.1:9099', update, 'POST', {'localId': second_uid, 'disableUser': False}, 'owner')
    report = {'result': 'PASS', 'verifiedAt': datetime.now(timezone.utc).isoformat(),
        'apiUrl': api_url, 'firebaseProject': 'demo-climbbetter', 'profiles': profiles,
        'seededSessionIds': sessions, 'seededEntries': 8,
        'checks': ['missing/invalid token', 'Admin SDK verification', 'idempotent bootstrap', 'profile update',
            'two accounts', 'training ownership', 'different project rejected', 'disabled account rejected']}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'result': 'PASS', 'modelSessions': len(sessions), 'modelEntries': 8, 'checks': len(report['checks'])}))


if __name__ == '__main__':
    main()
