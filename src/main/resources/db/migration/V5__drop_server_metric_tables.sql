-- flyway:executeInTransaction=false

DROP TABLE IF EXISTS server_docker_container_metrics CASCADE;
DROP TABLE IF EXISTS server_zfs_pool_metrics CASCADE;
DROP TABLE IF EXISTS server_zfs_arc_metrics CASCADE;
DROP TABLE IF EXISTS server_network_metrics CASCADE;
DROP TABLE IF EXISTS server_disk_metrics CASCADE;
DROP TABLE IF EXISTS server_metrics CASCADE;

DROP SEQUENCE IF EXISTS server_docker_container_metrics_id_seq;
DROP SEQUENCE IF EXISTS server_zfs_pool_metrics_id_seq;
DROP SEQUENCE IF EXISTS server_zfs_arc_metrics_id_seq;
DROP SEQUENCE IF EXISTS server_network_metrics_id_seq;
DROP SEQUENCE IF EXISTS server_disk_metrics_id_seq;
DROP SEQUENCE IF EXISTS server_metrics_id_seq;
