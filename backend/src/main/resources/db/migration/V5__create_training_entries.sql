ALTER TABLE training.training_sessions
    ALTER COLUMN classic_load TYPE NUMERIC(16, 4),
    ALTER COLUMN adjusted_load TYPE NUMERIC(16, 4);

CREATE TABLE training.training_entries (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL
        REFERENCES training.training_sessions(id)
        ON DELETE CASCADE,

    client_entry_id UUID NOT NULL,
    entry_order INTEGER NOT NULL,

    result_type TEXT NOT NULL,
    climb_type TEXT,
    ascent_mode TEXT,
    protection_mode TEXT,

    climb_name_snapshot TEXT,
    grade_label TEXT,
    grade_points NUMERIC(12, 4),
    base_edl NUMERIC(12, 4),

    total_moves INTEGER,
    executed_moves INTEGER NOT NULL,

    edl_count NUMERIC(16, 10),
    move_intensity NUMERIC(16, 10),
    style_multiplier NUMERIC(8, 4),
    relative_effort_multiplier NUMERIC(8, 4),

    classic_load NUMERIC(16, 4) NOT NULL,
    adjusted_load NUMERIC(16, 4) NOT NULL,
    calculation_model_version TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT training_entries_order_non_negative
        CHECK (entry_order >= 0),

    CONSTRAINT training_entries_result_type_valid
        CHECK (
            result_type IN ('ASCENT', 'ATTEMPT', 'WARMUP')
        ),

    CONSTRAINT training_entries_climb_type_valid
        CHECK (
            climb_type IS NULL
            OR climb_type IN ('BOULDER', 'ROUTE', 'CIRCUIT')
        ),

    CONSTRAINT training_entries_ascent_mode_valid
        CHECK (
            ascent_mode IS NULL
            OR ascent_mode IN ('OS', 'FLASH', 'RP')
        ),

    CONSTRAINT training_entries_protection_mode_valid
        CHECK (
            protection_mode IS NULL
            OR protection_mode IN ('LEAD', 'TOP_ROPE')
        ),

    CONSTRAINT training_entries_executed_moves_non_negative
        CHECK (executed_moves >= 0),

    CONSTRAINT training_entries_loads_non_negative
        CHECK (
            classic_load >= 0
            AND adjusted_load >= 0
        ),

    CONSTRAINT training_entries_variant_valid
        CHECK (
            (
                result_type = 'WARMUP'
                AND executed_moves > 0
                AND climb_type IS NULL
                AND ascent_mode IS NULL
                AND protection_mode IS NULL
                AND climb_name_snapshot IS NULL
                AND grade_label IS NULL
                AND grade_points IS NULL
                AND base_edl IS NULL
                AND total_moves IS NULL
                AND edl_count IS NULL
                AND move_intensity IS NULL
                AND style_multiplier IS NULL
                AND relative_effort_multiplier IS NULL
                AND classic_load = 0
                AND adjusted_load = 0
            )
            OR
            (
                result_type IN ('ASCENT', 'ATTEMPT')
                AND climb_type IS NOT NULL
                AND ascent_mode IS NOT NULL
                AND climb_name_snapshot IS NOT NULL
                AND length(trim(climb_name_snapshot)) > 0
                AND grade_label IS NOT NULL
                AND length(trim(grade_label)) > 0
                AND grade_points IS NOT NULL
                AND grade_points > 0
                AND base_edl IS NOT NULL
                AND base_edl >= 0
                AND total_moves IS NOT NULL
                AND total_moves > 0
                AND edl_count IS NOT NULL
                AND edl_count > 0
                AND move_intensity IS NOT NULL
                AND move_intensity > 0
                AND style_multiplier IS NOT NULL
                AND style_multiplier >= 0
                AND relative_effort_multiplier IS NOT NULL
                AND relative_effort_multiplier >= 0
                AND (
                    (
                        climb_type = 'ROUTE'
                        AND protection_mode IS NOT NULL
                    )
                    OR
                    (
                        climb_type <> 'ROUTE'
                        AND protection_mode IS NULL
                    )
                )
            )
        ),

    CONSTRAINT training_entries_session_client_id_unique
        UNIQUE (session_id, client_entry_id),

    CONSTRAINT training_entries_session_order_unique
        UNIQUE (session_id, entry_order)
);

CREATE INDEX training_entries_session_order_index
    ON training.training_entries (session_id, entry_order);