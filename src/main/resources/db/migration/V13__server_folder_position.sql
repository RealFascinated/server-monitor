ALTER TABLE server_folders
    ADD COLUMN position INT NOT NULL DEFAULT 0;

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY LOWER(name)) - 1 AS pos
    FROM server_folders
)
UPDATE server_folders AS sf
SET position = ranked.pos
FROM ranked
WHERE sf.id = ranked.id;
