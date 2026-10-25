-- Long-lived tokens for MCP clients. Separate from user_sessions: a session token never works on /mcp and an API
-- token never works on the REST API. Only the hash is stored; the raw token is shown once at creation.
CREATE TABLE api_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    token_hash    TEXT NOT NULL UNIQUE,
    token_prefix  TEXT NOT NULL,
    scopes        TEXT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    expires_at    TIMESTAMPTZ,
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ
);

CREATE INDEX api_tokens_user_id_idx ON api_tokens (user_id);
