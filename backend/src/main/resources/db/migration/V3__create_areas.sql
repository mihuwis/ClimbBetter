CREATE TABLE catalog.areas (
    id UUID PRIMARY KEY,
    client_area_id UUID,
    owner_id UUID REFERENCES identity.users(id),

    name TEXT NOT NULL,
    area_type TEXT NOT NULL,
    facility_type TEXT,

    resolution_status TEXT NOT NULL,
    is_public BOOLEAN NOT NULL,
    is_archived BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT areas_name_not_blank
        CHECK (length(trim(name)) > 0),

    CONSTRAINT areas_area_type_valid
        CHECK (area_type IN ('ARTIFICIAL', 'ROCK', 'OTHER')),

    CONSTRAINT areas_facility_type_valid
        CHECK (
            facility_type IS NULL
            OR facility_type IN (
                'BOULDER_GYM',
                'ROPE_GYM',
                'MIXED_GYM',
                'HOME_WALL',
                'BOARD'
            )
        ),

    CONSTRAINT areas_resolution_status_valid
        CHECK (resolution_status IN ('DRAFT', 'RESOLVED', 'MERGED')),

    CONSTRAINT areas_client_id_requires_owner
        CHECK (client_area_id IS NULL OR owner_id IS NOT NULL),

    CONSTRAINT areas_draft_requires_owner
        CHECK (resolution_status <> 'DRAFT' OR owner_id IS NOT NULL)
);

CREATE UNIQUE INDEX areas_owner_client_area_id_unique
    ON catalog.areas (owner_id, client_area_id)
    WHERE client_area_id IS NOT NULL;