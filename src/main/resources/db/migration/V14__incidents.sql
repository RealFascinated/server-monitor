CREATE TABLE incidents (
    id          BIGSERIAL PRIMARY KEY,
    server_id   BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    started_at  TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX incidents_server_id_started_at_idx ON incidents (server_id, started_at DESC);
CREATE UNIQUE INDEX incidents_server_id_open_idx ON incidents (server_id) WHERE resolved_at IS NULL;
