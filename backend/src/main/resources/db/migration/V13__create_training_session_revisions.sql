CREATE TABLE training.training_session_revisions (
    id UUID PRIMARY KEY,

    session_id UUID NOT NULL
        REFERENCES training.training_sessions(id),

    changed_by_user_id UUID NOT NULL
        REFERENCES identity.users(id),

    revision_number BIGINT NOT NULL,
    change_reason TEXT NOT NULL,

    before_state JSONB NOT NULL,
    after_state JSONB NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT training_session_revisions_number_positive
        CHECK (revision_number > 0),

    CONSTRAINT training_session_revisions_reason_not_blank
        CHECK (length(trim(change_reason)) > 0),

    CONSTRAINT training_session_revisions_session_number_unique
        UNIQUE (session_id, revision_number)
);

CREATE INDEX training_session_revisions_session_index
    ON training.training_session_revisions (
        session_id,
        revision_number DESC
    );