ALTER TABLE training.training_sessions
    ADD COLUMN request_fingerprint VARCHAR(64);

ALTER TABLE training.training_sessions
    ADD CONSTRAINT training_sessions_request_fingerprint_valid
        CHECK (
            request_fingerprint IS NULL
            OR request_fingerprint
                ~ '^[0-9a-f]{64}$'
        );