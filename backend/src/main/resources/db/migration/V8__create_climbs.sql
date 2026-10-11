CREATE TABLE catalog.climbs (
    id UUID PRIMARY KEY,
    client_climb_id UUID,
    owner_id UUID
        REFERENCES identity.users(id),

    area_id UUID NOT NULL
        REFERENCES catalog.areas(id),

    name TEXT NOT NULL,
    climb_type TEXT NOT NULL,

    grade_id UUID NOT NULL
        REFERENCES catalog.grades(id),

    default_effort_profile_id UUID
        REFERENCES training.effort_profiles(id),

    suggested_move_count INTEGER,

    is_public BOOLEAN NOT NULL,
    is_verified BOOLEAN NOT NULL,
    is_archived BOOLEAN NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT climbs_name_not_blank
        CHECK (length(trim(name)) > 0),

    CONSTRAINT climbs_type_valid
        CHECK (
            climb_type IN ('BOULDER', 'ROUTE', 'CIRCUIT')
        ),

    CONSTRAINT climbs_suggested_move_count_positive
        CHECK (
            suggested_move_count IS NULL
            OR suggested_move_count > 0
        ),

    CONSTRAINT climbs_client_id_requires_owner
        CHECK (
            client_climb_id IS NULL
            OR owner_id IS NOT NULL
        )
);

CREATE UNIQUE INDEX climbs_owner_client_climb_id_unique
    ON catalog.climbs (owner_id, client_climb_id)
    WHERE client_climb_id IS NOT NULL;

CREATE INDEX climbs_area_name_index
    ON catalog.climbs (area_id, name);

ALTER TABLE training.training_entries
    ADD COLUMN climb_id UUID
        REFERENCES catalog.climbs(id);

ALTER TABLE training.training_entries
    ADD CONSTRAINT training_entries_climb_reference_by_variant
        CHECK (
            (
                result_type = 'WARMUP'
                AND climb_id IS NULL
            )
            OR
            (
                result_type IN ('ASCENT', 'ATTEMPT')
                AND climb_id IS NOT NULL
            )
        );

CREATE INDEX training_entries_climb_id_index
    ON training.training_entries (climb_id)
    WHERE climb_id IS NOT NULL;