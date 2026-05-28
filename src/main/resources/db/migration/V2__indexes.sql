CREATE INDEX idx_server_metrics_server_time
    ON server_metrics (server_id, timestamp DESC);

CREATE INDEX idx_server_disk_metrics_server_disk_time
    ON server_disk_metrics (server_id, disk_name, timestamp DESC);

CREATE INDEX idx_server_network_metrics_server_iface_time
    ON server_network_metrics (server_id, interface_name, timestamp DESC);

CREATE INDEX idx_server_ingest_tokens_token_hash
    ON server_ingest_tokens (token_hash);
