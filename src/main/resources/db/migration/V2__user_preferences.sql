CREATE TABLE user_preferences (
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    key           TEXT NOT NULL,
    value         JSONB NOT NULL,
    last_modified TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, key)
);
