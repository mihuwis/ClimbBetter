CREATE TABLE training.training_sessions (
    id UUID PRIMARY KEY,
    client_session_id UUID NOT NULL,

    user_id UUID NOT NULL
        REFERENCES identity.users(id),

    area_id UUID NOT NULL
        REFERENCES catalog.areas(id),

    session_name TEXT NOT NULL,
    session_date DATE NOT NULL,
    time_zone_id TEXT NOT NULL,
    duration_minutes INTEGER NOT NULL,
    location_name_snapshot TEXT NOT NULL,

    total_moves INTEGER NOT NULL,
    classic_load NUMERIC(12, 2) NOT NULL,
    adjusted_load NUMERIC(12, 2) NOT NULL,

    max_boulder_grade TEXT,
    max_route_grade TEXT,

    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL,

    CONSTRAINT training_sessions_name_not_blank
        CHECK (length(trim(session_name)) > 0),

    CONSTRAINT training_sessions_time_zone_not_blank
        CHECK (length(trim(time_zone_id)) > 0),

    CONSTRAINT training_sessions_location_not_blank
        CHECK (length(trim(location_name_snapshot)) > 0),

    CONSTRAINT training_sessions_duration_positive
        CHECK (duration_minutes > 0),

    CONSTRAINT training_sessions_total_moves_non_negative
        CHECK (total_moves >= 0),

    CONSTRAINT training_sessions_classic_load_non_negative
        CHECK (classic_load >= 0),

    CONSTRAINT training_sessions_adjusted_load_non_negative
        CHECK (adjusted_load >= 0),

    CONSTRAINT training_sessions_has_completed_grade
        CHECK (
            max_boulder_grade IS NOT NULL
            OR max_route_grade IS NOT NULL
        ),

    CONSTRAINT training_sessions_user_client_id_unique
        UNIQUE (user_id, client_session_id)
);

CREATE INDEX training_sessions_user_date_index
    ON training.training_sessions (user_id, session_date DESC);