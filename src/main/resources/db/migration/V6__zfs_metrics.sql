CREATE TABLE server_zfs_arc_metrics (
    id                   BIGSERIAL PRIMARY KEY,
    server_id            BIGINT REFERENCES servers (id),
    arc_size_bytes       BIGINT,
    arc_target_bytes     BIGINT,
    arc_max_bytes        BIGINT,
    arc_min_bytes        BIGINT,
    arc_data_bytes       BIGINT,
    arc_metadata_bytes   BIGINT,
    l2arc_size_bytes     BIGINT,
    arc_hit_ratio        DOUBLE PRECISION,
    arc_misses_per_second BIGINT,
    timestamp            TIMESTAMPTZ
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
