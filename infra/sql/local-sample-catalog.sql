-- Local development data only. Run directly against the Compose database.
-- Stable seed IDs make reruns safe without overwriting edited catalog entries.
SET client_encoding = 'UTF8';
BEGIN;

INSERT INTO identity.users (id, display_name, created_at)
VALUES ('00000000-0000-0000-0000-000000000002', 'Mihu', now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO catalog.areas (
    id, owner_id, name, area_type, facility_type, resolution_status,
    is_public, is_archived, created_at, updated_at, version
)
VALUES
    (
        md5('LOCAL_SAMPLE:AREA:BRONX')::uuid,
        '00000000-0000-0000-0000-000000000002',
        'Bronx', 'ARTIFICIAL', 'BOULDER_GYM', 'RESOLVED',
        true, false, now(), now(), 0
    ),
    (
        md5('LOCAL_SAMPLE:AREA:GARAGE')::uuid,
        '00000000-0000-0000-0000-000000000002',
        'Garaż', 'ARTIFICIAL', 'HOME_WALL', 'RESOLVED',
        false, false, now(), now(), 0
    )
ON CONFLICT (id) DO NOTHING;

WITH seed (seed_key, area_key, name, climb_type, scale_code, grade_label,
           profile_code, moves) AS (
    VALUES
        ('BRONX_GREEN_SLOPERS', 'BRONX', 'Zielona po glizdach',
         'BOULDER', 'BOULDER_FONT', '5', 'BOULDER', 8),
        ('BRONX_GREEN_BALLS', 'BRONX', 'Zielona kule',
         'BOULDER', 'BOULDER_FONT', '5+', 'BOULDER', 8),
        ('BRONX_BLUE_CORNER', 'BRONX', 'niebieska w rogu',
         'BOULDER', 'BOULDER_FONT', '6A', 'BOULDER', 8),
        ('BRONX_BLUE_01', 'BRONX', 'niebieski01',
         'BOULDER', 'BOULDER_FONT', '6A', 'BOULDER', 8),
        ('BRONX_BLUE_02', 'BRONX', 'niebieski02',
         'BOULDER', 'BOULDER_FONT', '6A+', 'BOULDER', 8),
        ('BRONX_BLUE_03', 'BRONX', 'niebieski03',
         'BOULDER', 'BOULDER_FONT', '6B', 'BOULDER', 8),
        ('BRONX_BLUE_04', 'BRONX', 'niebieski04',
         'BOULDER', 'BOULDER_FONT', '6A', 'BOULDER', 8),
        ('BRONX_BLUE_05', 'BRONX', 'niebieski05',
         'BOULDER', 'BOULDER_FONT', '6A+', 'BOULDER', 8),
        ('BRONX_ORANGE_01', 'BRONX', 'Pomarańczowy01',
         'BOULDER', 'BOULDER_FONT', '6B', 'BOULDER', 8),
        ('BRONX_ORANGE_02', 'BRONX', 'Pomarańczowy02',
         'BOULDER', 'BOULDER_FONT', '6A', 'BOULDER', 8),
        ('BRONX_ORANGE_03', 'BRONX', 'Pomarańczowy03',
         'BOULDER', 'BOULDER_FONT', '6A+', 'BOULDER', 8),
        ('BRONX_ORANGE_04', 'BRONX', 'Pomarańczowy04',
         'BOULDER', 'BOULDER_FONT', '6B', 'BOULDER', 8),
        ('BRONX_ORANGE_05', 'BRONX', 'Pomarańczowy05',
         'BOULDER', 'BOULDER_FONT', '6A', 'BOULDER', 8),
        ('GARAGE_GREEN_BASE', 'GARAGE', 'zielony bazowy',
         'CIRCUIT', 'ROUTE_FRENCH', '6a', 'CIRCUIT', 38),
        ('GARAGE_PURPLE_BASE', 'GARAGE', 'fiolet bazowy',
         'CIRCUIT', 'ROUTE_FRENCH', '6b', 'CIRCUIT', 38),
        ('GARAGE_ORANGE_BASE', 'GARAGE', 'pomarańcz bazowy',
         'CIRCUIT', 'ROUTE_FRENCH', '6c', 'CIRCUIT', 38)
)
INSERT INTO catalog.climbs (
    id, owner_id, area_id, name, climb_type, grade_id,
    default_effort_profile_id, suggested_move_count,
    is_public, is_verified, is_archived, created_at, updated_at, version
)
SELECT
    md5('LOCAL_SAMPLE:CLIMB:' || seed.seed_key)::uuid,
    '00000000-0000-0000-0000-000000000002'::uuid,
    md5('LOCAL_SAMPLE:AREA:' || seed.area_key)::uuid,
    seed.name,
    seed.climb_type,
    (SELECT id FROM catalog.grades
     WHERE scale_code = seed.scale_code AND label = seed.grade_label AND is_active),
    (SELECT id FROM training.effort_profiles
     WHERE code = seed.profile_code AND is_active),
    seed.moves,
    false, false, false, now(), now(), 0
FROM seed
ON CONFLICT (id) DO NOTHING;

COMMIT;
