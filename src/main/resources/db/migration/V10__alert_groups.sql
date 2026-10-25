-- Alert rules belong to a group instead of a server; servers inherit the rules of every group they are in.
CREATE TABLE alert_groups (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name              TEXT NOT NULL,
    auto_add_servers  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL
);

CREATE INDEX alert_groups_user_id_idx ON alert_groups (user_id);

CREATE TABLE alert_group_servers (
    group_id   BIGINT NOT NULL REFERENCES alert_groups (id) ON DELETE CASCADE,
    server_id  BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, server_id)
);

CREATE INDEX alert_group_servers_server_id_idx ON alert_group_servers (server_id);

ALTER TABLE alert_rules ADD COLUMN group_id BIGINT REFERENCES alert_groups (id) ON DELETE CASCADE;

-- Per-server rules are gone; account-wide rules move into a Default group holding all the owner's servers.
DELETE FROM alert_rules WHERE server_id IS NOT NULL;

INSERT INTO alert_groups (user_id, name, auto_add_servers, created_at)
SELECT DISTINCT user_id, 'Default', TRUE, NOW() FROM alert_rules;

UPDATE alert_rules r SET group_id = g.id FROM alert_groups g WHERE g.user_id = r.user_id;

INSERT INTO alert_group_servers (group_id, server_id)
SELECT g.id, m.server_id FROM alert_groups g JOIN server_members m ON m.user_id = g.user_id;

ALTER TABLE alert_rules ALTER COLUMN group_id SET NOT NULL;
DROP INDEX IF EXISTS alert_rules_server_id_idx;
ALTER TABLE alert_rules DROP COLUMN server_id;
CREATE INDEX alert_rules_group_id_idx ON alert_rules (group_id);
