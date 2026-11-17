-- A group can notify several destinations.
CREATE TABLE alert_group_destinations (
    group_id       BIGINT NOT NULL REFERENCES alert_groups(id) ON DELETE CASCADE,
    destination_id BIGINT NOT NULL REFERENCES alert_destinations(id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, destination_id)
);

INSERT INTO alert_group_destinations (group_id, destination_id)
SELECT id, destination_id FROM alert_groups WHERE destination_id IS NOT NULL;

ALTER TABLE alert_groups DROP COLUMN destination_id;
