CREATE TABLE server_inventory (
    server_id       BIGINT PRIMARY KEY REFERENCES servers (id) ON DELETE CASCADE,
    ip              TEXT,
    core_count      INTEGER,
    thread_count    INTEGER,
    cpu_model       TEXT,
    socket_count    INTEGER,
    cpu_clock_mhz   DOUBLE PRECISION,
    os_name         TEXT,
    os_version      TEXT
);

INSERT INTO server_inventory (
    server_id,
    ip,
    core_count,
    thread_count,
    cpu_model,
    socket_count,
    cpu_clock_mhz,
    os_name,
    os_version
)
SELECT
    id,
    ip,
    core_count,
    thread_count,
    cpu_model,
    socket_count,
    cpu_clock_mhz,
    os_name,
    os_version
FROM servers;

ALTER TABLE servers
    DROP COLUMN ip,
    DROP COLUMN core_count,
    DROP COLUMN thread_count,
    DROP COLUMN cpu_model,
    DROP COLUMN socket_count,
    DROP COLUMN cpu_clock_mhz,
    DROP COLUMN os_name,
    DROP COLUMN os_version;
