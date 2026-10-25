-- Groups can be switched off without deleting them, and every user keeps one undeletable default group.
ALTER TABLE alert_groups ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE alert_groups ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT FALSE;

-- The Default group V10 seeded is each user's earliest one with that name.
UPDATE alert_groups g SET is_default = TRUE
WHERE g.name = 'Default'
  AND g.id = (SELECT MIN(g2.id) FROM alert_groups g2 WHERE g2.user_id = g.user_id AND g2.name = 'Default');

CREATE UNIQUE INDEX alert_groups_one_default_per_user_idx ON alert_groups (user_id) WHERE is_default;
