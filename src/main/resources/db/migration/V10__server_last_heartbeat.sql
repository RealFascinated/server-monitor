ALTER TABLE servers
    ADD COLUMN last_heartbeat TIMESTAMPTZ;

UPDATE servers
SET last_heartbeat = last_updated
WHERE last_updated IS NOT NULL;
