INSERT INTO server_members (server_id, user_id, role, created_at)
SELECT id, owner_id, 'OWNER', created_at FROM servers;

CREATE UNIQUE INDEX server_members_one_owner_idx
    ON server_members (server_id) WHERE role = 'OWNER';

ALTER TABLE servers DROP COLUMN owner_id;
