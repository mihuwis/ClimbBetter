INSERT INTO catalog.areas (
    id,
    client_area_id,
    owner_id,
    name,
    area_type,
    facility_type,
    resolution_status,
    is_public,
    is_archived,
    created_at,
    updated_at,
    version
)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    NULL,
    NULL,
    'My Gym',
    'ARTIFICIAL',
    'MIXED_GYM',
    'RESOLVED',
    TRUE,
    FALSE,
    TIMESTAMPTZ '2026-10-10 18:00:00+00',
    TIMESTAMPTZ '2026-10-10 18:00:00+00',
    0
);

INSERT INTO training.training_sessions (
    id,
    client_session_id,
    user_id,
    area_id,
    session_name,
    session_date,
    time_zone_id,
    duration_minutes,
    location_name_snapshot,
    total_moves,
    classic_load,
    adjusted_load,
    max_boulder_grade,
    max_route_grade,
    created_at,
    updated_at,
    version
)
VALUES (
    '20000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'Evening Training',
    DATE '2026-10-10',
    'Europe/Warsaw',
    90,
    'My Gym',
    120,
    850.00,
    920.50,
    '7A',
    '7c',
    TIMESTAMPTZ '2026-10-10 18:00:00+00',
    TIMESTAMPTZ '2026-10-10 18:00:00+00',
    0
);