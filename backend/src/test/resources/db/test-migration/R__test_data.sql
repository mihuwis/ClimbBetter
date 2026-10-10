INSERT INTO identity.users (
    id,
    display_name,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Local Developer',
    TIMESTAMPTZ '2026-01-01 00:00:00+00'
)
ON CONFLICT (id) DO NOTHING;