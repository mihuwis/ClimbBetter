CREATE TABLE identity.users (
    id UUID PRIMARY KEY,
    display_name TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT users_display_name_not_blank
        CHECK (length(trim(display_name)) > 0)
);