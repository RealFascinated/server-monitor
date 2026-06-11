CREATE TABLE server_members (
    server_id   BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (server_id, user_id)
);

CREATE TABLE server_invites (
    id              BIGSERIAL PRIMARY KEY,
    server_id       BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    email           TEXT NOT NULL,
    role            TEXT NOT NULL,
    token_hash      TEXT NOT NULL UNIQUE,
    invited_by_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (server_id, email)
);

CREATE INDEX server_invites_email_idx ON server_invites (LOWER(email));
