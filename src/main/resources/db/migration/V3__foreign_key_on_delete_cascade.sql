ALTER TABLE servers
    DROP CONSTRAINT servers_owner_id_fkey,
    ADD CONSTRAINT servers_owner_id_fkey
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE;

ALTER TABLE server_ingest_tokens
    DROP CONSTRAINT server_ingest_tokens_server_id_fkey,
    ADD CONSTRAINT server_ingest_tokens_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_metrics
    DROP CONSTRAINT server_metrics_server_id_fkey,
    ADD CONSTRAINT server_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_disk_metrics
    DROP CONSTRAINT server_disk_metrics_server_id_fkey,
    ADD CONSTRAINT server_disk_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_network_metrics
    DROP CONSTRAINT server_network_metrics_server_id_fkey,
    ADD CONSTRAINT server_network_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_zfs_arc_metrics
    DROP CONSTRAINT server_zfs_arc_metrics_server_id_fkey,
    ADD CONSTRAINT server_zfs_arc_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_zfs_pool_metrics
    DROP CONSTRAINT server_zfs_pool_metrics_server_id_fkey,
    ADD CONSTRAINT server_zfs_pool_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;

ALTER TABLE server_docker_container_metrics
    DROP CONSTRAINT server_docker_container_metrics_server_id_fkey,
    ADD CONSTRAINT server_docker_container_metrics_server_id_fkey
        FOREIGN KEY (server_id) REFERENCES servers (id) ON DELETE CASCADE;
