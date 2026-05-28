ALTER TABLE server_metrics
    ADD COLUMN mem_available DOUBLE PRECISION,
    ADD COLUMN process_count BIGINT,
    ADD COLUMN running_processes BIGINT,
    ADD COLUMN ctx_switches_per_second BIGINT,
    ADD COLUMN interrupts_per_second BIGINT;

ALTER TABLE server_disk_metrics
    ADD COLUMN inode_used BIGINT,
    ADD COLUMN inode_total BIGINT,
    ADD COLUMN read_iops BIGINT,
    ADD COLUMN write_iops BIGINT,
    ADD COLUMN read_latency_ms BIGINT,
    ADD COLUMN write_latency_ms BIGINT;
