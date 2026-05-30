-- flyway:executeInTransaction=false

ALTER TABLE server_metrics DROP CONSTRAINT server_metrics_pkey;
ALTER TABLE server_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

ALTER TABLE server_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_metrics', INTERVAL '7 days');

ALTER TABLE server_disk_metrics DROP CONSTRAINT server_disk_metrics_pkey;
ALTER TABLE server_disk_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_disk_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_disk_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

ALTER TABLE server_disk_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id, disk_name',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_disk_metrics', INTERVAL '7 days');

ALTER TABLE server_network_metrics DROP CONSTRAINT server_network_metrics_pkey;
ALTER TABLE server_network_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_network_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_network_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

ALTER TABLE server_network_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id, interface_name',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_network_metrics', INTERVAL '7 days');

ALTER TABLE server_zfs_arc_metrics DROP CONSTRAINT server_zfs_arc_metrics_pkey;
ALTER TABLE server_zfs_arc_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_zfs_arc_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_zfs_arc_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

CREATE INDEX idx_server_zfs_arc_metrics_server_time
    ON server_zfs_arc_metrics (server_id, timestamp DESC);

ALTER TABLE server_zfs_arc_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_zfs_arc_metrics', INTERVAL '7 days');

ALTER TABLE server_zfs_pool_metrics DROP CONSTRAINT server_zfs_pool_metrics_pkey;
ALTER TABLE server_zfs_pool_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_zfs_pool_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_zfs_pool_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

CREATE INDEX idx_server_zfs_pool_metrics_server_pool_time
    ON server_zfs_pool_metrics (server_id, pool_name, timestamp DESC);

ALTER TABLE server_zfs_pool_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id, pool_name',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_zfs_pool_metrics', INTERVAL '7 days');

ALTER TABLE server_docker_container_metrics DROP CONSTRAINT server_docker_container_metrics_pkey;
ALTER TABLE server_docker_container_metrics ALTER COLUMN timestamp SET NOT NULL;
ALTER TABLE server_docker_container_metrics ADD PRIMARY KEY (timestamp, id);

SELECT create_hypertable('server_docker_container_metrics', 'timestamp', chunk_time_interval => INTERVAL '1 day', migrate_data => true);

ALTER TABLE server_docker_container_metrics SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'server_id, container_name',
    timescaledb.compress_orderby = 'timestamp DESC'
);
SELECT add_compression_policy('server_docker_container_metrics', INTERVAL '7 days');
