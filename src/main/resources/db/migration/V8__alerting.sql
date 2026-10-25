CREATE TABLE alert_destinations (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name              TEXT NOT NULL,
    type              TEXT NOT NULL,
    config_encrypted  TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX alert_destinations_user_id_idx ON alert_destinations (user_id);

CREATE TABLE alert_rules (
    id                       BIGSERIAL PRIMARY KEY,
    user_id                  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    server_id                BIGINT REFERENCES servers (id) ON DELETE CASCADE,
    destination_id           BIGINT NOT NULL REFERENCES alert_destinations (id) ON DELETE CASCADE,
    name                     TEXT,
    metric                   TEXT NOT NULL,
    label_filter             JSONB NOT NULL DEFAULT '{}',
    operator                 TEXT NOT NULL,
    threshold                DOUBLE PRECISION NOT NULL,
    recovery_threshold       DOUBLE PRECISION,
    window_seconds           INT NOT NULL DEFAULT 0,
    severity                 TEXT NOT NULL DEFAULT 'WARNING',
    repeat_interval_seconds  INT,
    enabled                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ NOT NULL
);

CREATE INDEX alert_rules_user_id_idx ON alert_rules (user_id);
CREATE INDEX alert_rules_server_id_idx ON alert_rules (server_id);
CREATE INDEX alert_rules_destination_id_idx ON alert_rules (destination_id);

CREATE TABLE alert_events (
    id                BIGSERIAL PRIMARY KEY,
    rule_id           BIGINT NOT NULL REFERENCES alert_rules (id) ON DELETE CASCADE,
    server_id         BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    series_key        TEXT NOT NULL,
    started_at        TIMESTAMPTZ NOT NULL,
    resolved_at       TIMESTAMPTZ,
    resolve_reason    TEXT,
    peak_value        DOUBLE PRECISION NOT NULL,
    last_notified_at  TIMESTAMPTZ,
    notify_error      TEXT
);

CREATE INDEX alert_events_rule_started_idx ON alert_events (rule_id, started_at DESC);
CREATE INDEX alert_events_server_started_idx ON alert_events (server_id, started_at DESC);
CREATE INDEX alert_events_open_idx ON alert_events (rule_id, server_id) WHERE resolved_at IS NULL;
