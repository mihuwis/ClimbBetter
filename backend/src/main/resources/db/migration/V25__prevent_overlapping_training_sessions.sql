CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE training.training_sessions
    ALTER COLUMN started_at SET NOT NULL,
    ALTER COLUMN ended_at SET NOT NULL;

-- A user can finish one session exactly when the next one starts.
-- Keep the check in PostgreSQL so concurrent inserts and edits cannot bypass it.
ALTER TABLE training.training_sessions
    ADD CONSTRAINT training_sessions_user_time_no_overlap
        EXCLUDE USING gist (
            user_id WITH =,
            tstzrange(started_at, ended_at, '[)') WITH &&
        )
        WHERE (deleted_at IS NULL);
