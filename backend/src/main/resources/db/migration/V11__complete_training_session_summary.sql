ALTER TABLE training.training_sessions
    DROP CONSTRAINT training_sessions_has_completed_grade,

    ADD COLUMN started_at TIMESTAMPTZ,

    ADD COLUMN ended_at TIMESTAMPTZ,

    ADD COLUMN average_move_intensity NUMERIC(16, 10);

ALTER TABLE training.training_sessions
    ADD CONSTRAINT training_sessions_time_range_valid
        CHECK (
            (
                started_at IS NULL
                AND ended_at IS NULL
            )
            OR
            (
                started_at IS NOT NULL
                AND ended_at IS NOT NULL
                AND ended_at > started_at
            )
        ),

    ADD CONSTRAINT training_sessions_average_intensity_positive
        CHECK (
            average_move_intensity IS NULL
            OR average_move_intensity > 0
        );