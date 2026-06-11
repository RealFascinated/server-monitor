-- PENDING = ordinal 2 (ONLINE=0, OFFLINE=1)
UPDATE servers
SET status = 2
WHERE last_updated IS NULL
  AND (status IS NULL OR status = 1);
