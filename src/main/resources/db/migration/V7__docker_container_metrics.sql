CREATE TABLE server_docker_container_metrics (
    id              BIGSERIAL PRIMARY KEY,
    server_id       BIGINT REFERENCES servers (id),
    container_name  TEXT,
    cpu_usage       BIGINT,
    memory_usage    BIGINT,
    timestamp       TIMESTAMPTZ
);

CREATE INDEX idx_server_docker_container_metrics_server_container_time
    ON server_docker_container_metrics (server_id, container_name, timestamp DESC);
