CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL
);

CREATE TABLE user_sessions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  TEXT NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE servers (
    id                   BIGSERIAL PRIMARY KEY,
    server_name          TEXT NOT NULL,
    status               INTEGER,
    last_updated         TIMESTAMPTZ,
    last_heartbeat       TIMESTAMPTZ,
    last_uptime_seconds  BIGINT,
    agent_version        TEXT,
    created_at           TIMESTAMPTZ NOT NULL
);

CREATE TABLE server_inventory (
    server_id       BIGINT PRIMARY KEY REFERENCES servers (id) ON DELETE CASCADE,
    ip              TEXT,
    core_count      INTEGER,
    thread_count    INTEGER,
    cpu_model       TEXT,
    socket_count    INTEGER,
    os_name         TEXT,
    os_version      TEXT
);

CREATE TABLE server_ingest_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  TEXT,
    server_id   BIGINT REFERENCES servers (id) ON DELETE CASCADE
);

CREATE TABLE server_members (
    server_id   BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role        TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (server_id, user_id)
);

CREATE UNIQUE INDEX server_members_one_owner_idx
    ON server_members (server_id) WHERE role = 'OWNER';

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

CREATE TABLE settings (
    key            TEXT PRIMARY KEY,
    value          JSONB NOT NULL,
    last_modified  TIMESTAMPTZ NOT NULL
);

CREATE TABLE server_folders (
    id       BIGSERIAL PRIMARY KEY,
    name     TEXT NOT NULL,
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    position INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX server_folders_user_id_name_idx ON server_folders (user_id, LOWER(name));

CREATE TABLE server_folder_assignments (
    id        BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES server_folders (id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    UNIQUE (folder_id, server_id)
);

CREATE TABLE incidents (
    id          BIGSERIAL PRIMARY KEY,
    server_id   BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    started_at  TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_server_ingest_tokens_token_hash ON server_ingest_tokens (token_hash);
CREATE INDEX user_sessions_token_hash_idx ON user_sessions (token_hash);
CREATE INDEX user_sessions_expires_at_idx ON user_sessions (expires_at);
CREATE INDEX server_invites_email_idx ON server_invites (LOWER(email));
CREATE INDEX server_folder_assignments_server_id_idx ON server_folder_assignments (server_id);
CREATE INDEX incidents_server_id_started_at_idx ON incidents (server_id, started_at DESC);
CREATE UNIQUE INDEX incidents_server_id_open_idx ON incidents (server_id) WHERE resolved_at IS NULL;
