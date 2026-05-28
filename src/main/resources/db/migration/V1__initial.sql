CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE servers (
    id                   BIGSERIAL PRIMARY KEY,
    server_name          TEXT NOT NULL,
    ip                   TEXT,
    core_count           INTEGER,
    thread_count         INTEGER,
    os_name              TEXT,
    os_version           TEXT,
    status               INTEGER,
    last_updated         TIMESTAMPTZ,
    last_uptime_seconds  BIGINT,
    created_at           TIMESTAMPTZ NOT NULL
);

CREATE TABLE server_ingest_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  TEXT,
    server_id   BIGINT REFERENCES servers (id)
);

CREATE TABLE server_metrics (
    id          BIGSERIAL PRIMARY KEY,
    server_id   BIGINT REFERENCES servers (id),
    cpu_usage   DOUBLE PRECISION,
    mem_usage   DOUBLE PRECISION,
    mem_total   DOUBLE PRECISION,
    timestamp   TIMESTAMPTZ
);

CREATE TABLE server_disk_metrics (
    id              BIGSERIAL PRIMARY KEY,
    server_id       BIGINT REFERENCES servers (id),
    disk_name       TEXT,
    usage_pct       DOUBLE PRECISION,
    used_bytes      BIGINT,
    total_bytes     BIGINT,
    io_read_bps     BIGINT,
    io_write_bps    BIGINT,
    io_usage_pct    DOUBLE PRECISION,
    io_wait_ms      DOUBLE PRECISION,
    timestamp       TIMESTAMPTZ
);

CREATE TABLE server_network_metrics (
    id                      BIGSERIAL PRIMARY KEY,
    server_id               BIGINT REFERENCES servers (id),
    interface_name          TEXT,
    rx_bps                  BIGINT,
    tx_bps                  BIGINT,
    rx_packets_per_second   BIGINT,
    tx_packets_per_second   BIGINT,
    rx_errors_per_second    BIGINT,
    tx_errors_per_second    BIGINT,
    timestamp               TIMESTAMPTZ
);
