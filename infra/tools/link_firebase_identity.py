"""Inspect or explicitly link one Firebase UID to an existing local ClimbBetter user."""
import argparse
import json
import re
import subprocess
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--project-id', required=True)
    parser.add_argument('--subject', required=True, help='UID verified in Firebase Console for the intended account')
    parser.add_argument('--user-id', type=uuid.UUID, default=uuid.UUID('00000000-0000-0000-0000-000000000002'))
    parser.add_argument('--container', default='climbbetter-postgres')
    parser.add_argument('--database', default='climbbetter')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    if not re.fullmatch(r'[a-z][a-z0-9-]{4,62}', args.project_id):
        parser.error('Invalid Firebase project ID')
    if not 1 <= len(args.subject) <= 128 or any(ord(char) < 32 for char in args.subject):
        parser.error('Invalid Firebase UID')
    issuer = 'https://securetoken.google.com/' + args.project_id
    subject = args.subject.replace("'", "''")
    user_id = str(args.user_id)
    block_tag = '$cb_link_' + uuid.uuid4().hex + '$'
    while block_tag in subject:
        block_tag = '$cb_link_' + uuid.uuid4().hex + '$'
    inspect = f"""
      SELECT json_build_object('userId',id,'displayName',display_name,
        'activeSessions',(SELECT count(*) FROM training.training_sessions WHERE user_id=identity.users.id AND deleted_at IS NULL),
        'existingIdentity',(SELECT subject FROM identity.user_identities WHERE user_id=identity.users.id AND issuer='{issuer}'))
      FROM identity.users WHERE id='{user_id}'::uuid;
    """
    statements = f"""
      SET standard_conforming_strings=on;
      BEGIN;
      SELECT pg_advisory_xact_lock(hashtextextended('{issuer}|{subject}',0));
      DO {block_tag} BEGIN
        IF NOT EXISTS(SELECT 1 FROM identity.users WHERE id='{user_id}'::uuid) THEN
          RAISE EXCEPTION 'Target CB user does not exist'; END IF;
        IF EXISTS(SELECT 1 FROM identity.user_identities WHERE issuer='{issuer}' AND subject='{subject}' AND user_id<>'{user_id}'::uuid)
          OR EXISTS(SELECT 1 FROM identity.user_identities WHERE issuer='{issuer}' AND user_id='{user_id}'::uuid AND subject<>'{subject}') THEN
          RAISE EXCEPTION 'Conflicting identity mapping; refusing to reassign an account'; END IF;
      END {block_tag};
      INSERT INTO identity.user_identities(id,user_id,issuer,subject,created_at)
      VALUES ('{uuid.uuid4()}'::uuid,'{user_id}'::uuid,'{issuer}','{subject}',now())
      ON CONFLICT (issuer,subject) DO NOTHING;
      COMMIT;
    """ if args.apply else ''
    result = subprocess.run(['docker','exec','-i',args.container,'psql','-U','climbbetter','-d',args.database,
        '-X','-q','-t','-A','-v','ON_ERROR_STOP=1'], input=statements + inspect,
        text=True, capture_output=True, check=True)
    rows = [line for line in result.stdout.splitlines() if line.strip().startswith('{')]
    if not rows:
        raise SystemExit('Target CB user does not exist; nothing linked.')
    summary = json.loads(rows[-1])
    summary['action'] = 'LINKED' if args.apply else 'INSPECTION ONLY; confirm the Firebase UID in Console before --apply'
    summary['projectId'] = args.project_id
    print(json.dumps(summary))


if __name__ == '__main__':
    main()
