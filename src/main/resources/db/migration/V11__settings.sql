CREATE TABLE settings (
    key            TEXT PRIMARY KEY,
    value          JSONB NOT NULL,
    last_modified  TIMESTAMPTZ NOT NULL
);
