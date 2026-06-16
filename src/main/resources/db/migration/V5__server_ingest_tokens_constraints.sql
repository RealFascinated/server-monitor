DELETE FROM server_ingest_tokens WHERE token_hash IS NULL OR server_id IS NULL;

DELETE FROM server_ingest_tokens t
USING server_ingest_tokens newer
WHERE t.server_id = newer.server_id
  AND t.id < newer.id;

DELETE FROM server_ingest_tokens t
USING server_ingest_tokens newer
WHERE t.token_hash = newer.token_hash
  AND t.id < newer.id;

ALTER TABLE server_ingest_tokens
    ALTER COLUMN token_hash SET NOT NULL,
    ALTER COLUMN server_id SET NOT NULL;

DROP INDEX idx_server_ingest_tokens_token_hash;

CREATE UNIQUE INDEX server_ingest_tokens_token_hash_idx ON server_ingest_tokens (token_hash);
CREATE UNIQUE INDEX server_ingest_tokens_server_id_idx ON server_ingest_tokens (server_id);
