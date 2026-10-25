-- A group notifies one destination for all of its alerts, instead of every alert picking its own.
ALTER TABLE alert_groups ADD COLUMN destination_id BIGINT REFERENCES alert_destinations(id) ON DELETE SET NULL;

-- Where a group's alerts used different destinations, keep the one most of them used.
UPDATE alert_groups g SET destination_id = (
    SELECT r.destination_id FROM alert_rules r
    WHERE r.group_id = g.id AND r.destination_id IS NOT NULL
    GROUP BY r.destination_id
    ORDER BY COUNT(*) DESC, r.destination_id ASC
    LIMIT 1
);

ALTER TABLE alert_rules DROP COLUMN destination_id;
