ALTER TABLE identity.users ADD COLUMN profile_completed_at TIMESTAMPTZ;

CREATE TABLE identity.user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES identity.users(id),
    issuer TEXT NOT NULL CHECK (length(trim(issuer)) > 0),
    subject TEXT NOT NULL CHECK (length(trim(subject)) > 0),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (issuer, subject),
    UNIQUE (user_id, issuer)
);
