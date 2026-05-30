CREATE EXTENSION IF NOT EXISTS timescaledb;

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
    owner_id             BIGINT NOT NULL REFERENCES users (id),
    ip                   TEXT,
    core_count           INTEGER,
    thread_count         INTEGER,
    cpu_model            TEXT,
    socket_count         INTEGER,
    cpu_clock_mhz        DOUBLE PRECISION,
    os_name              TEXT,
    os_version           TEXT,
    status               INTEGER,
    last_updated         TIMESTAMPTZ,
    last_uptime_seconds  BIGINT,
    agent_version        TEXT,
    created_at           TIMESTAMPTZ NOT NULL
);

CREATE TABLE server_ingest_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  TEXT,
    server_id   BIGINT REFERENCES servers (id)
);

CREATE TABLE server_metrics (
    id                          BIGSERIAL PRIMARY KEY,
    server_id                   BIGINT REFERENCES servers (id),
    cpu_usage                   DOUBLE PRECISION,
    mem_usage                   DOUBLE PRECISION,
    mem_total                   DOUBLE PRECISION,
    load_1                      DOUBLE PRECISION,
    load_5                      DOUBLE PRECISION,
    load_15                     DOUBLE PRECISION,
    cpu_user_pct                DOUBLE PRECISION,
    cpu_system_pct              DOUBLE PRECISION,
    cpu_iowait_pct              DOUBLE PRECISION,
    cpu_steal_pct               DOUBLE PRECISION,
    mem_buffers                 BIGINT,
    mem_cached                  BIGINT,
    swap_used                   BIGINT,
    swap_total                  BIGINT,
    mem_available               DOUBLE PRECISION,
    process_count               BIGINT,
    running_processes           BIGINT,
    ctx_switches_per_second     BIGINT,
    interrupts_per_second       BIGINT,
    timestamp                   TIMESTAMPTZ
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
    inode_used      BIGINT,
    inode_total     BIGINT,
    read_iops       BIGINT,
    write_iops      BIGINT,
    read_latency_ms BIGINT,
    write_latency_ms BIGINT,
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

CREATE TABLE server_zfs_arc_metrics (
    id                    BIGSERIAL PRIMARY KEY,
    server_id             BIGINT REFERENCES servers (id),
    arc_size_bytes        BIGINT,
    arc_target_bytes      BIGINT,
    arc_max_bytes         BIGINT,
    arc_min_bytes         BIGINT,
    arc_data_bytes        BIGINT,
    arc_metadata_bytes    BIGINT,
    l2arc_size_bytes      BIGINT,
    arc_hit_ratio         DOUBLE PRECISION,
    arc_misses_per_second BIGINT,
    timestamp             TIMESTAMPTZ
);

CREATE TABLE server_zfs_pool_metrics (
    id                    BIGSERIAL PRIMARY KEY,
    server_id             BIGINT REFERENCES servers (id),
    pool_name             TEXT,
    health                TEXT,
    capacity_percent      DOUBLE PRECISION,
    allocated_bytes       BIGINT,
    free_bytes            BIGINT,
    total_bytes           BIGINT,
    fragmentation_percent DOUBLE PRECISION,
    scan_state            TEXT,
    scan_percent          DOUBLE PRECISION,
    read_bps              BIGINT,
    write_bps             BIGINT,
    read_iops             BIGINT,
    write_iops            BIGINT,
    checksum_errors       BIGINT,
    timestamp             TIMESTAMPTZ
);

CREATE TABLE server_docker_container_metrics (
    id              BIGSERIAL PRIMARY KEY,
    server_id       BIGINT REFERENCES servers (id),
    container_name  TEXT,
    cpu_usage       DOUBLE PRECISION,
    memory_usage    BIGINT,
    timestamp       TIMESTAMPTZ
);

CREATE INDEX idx_server_metrics_server_time
    ON server_metrics (server_id, timestamp DESC);

CREATE INDEX idx_server_disk_metrics_server_disk_time
    ON server_disk_metrics (server_id, disk_name, timestamp DESC);

CREATE INDEX idx_server_network_metrics_server_iface_time
    ON server_network_metrics (server_id, interface_name, timestamp DESC);

CREATE INDEX idx_server_ingest_tokens_token_hash
    ON server_ingest_tokens (token_hash);

CREATE INDEX idx_server_docker_container_metrics_server_container_time
    ON server_docker_container_metrics (server_id, container_name, timestamp DESC);

CREATE INDEX user_sessions_token_hash_idx ON user_sessions (token_hash);
CREATE INDEX user_sessions_expires_at_idx ON user_sessions (expires_at);
