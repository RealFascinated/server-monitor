-- Default rules are created for every new user before they have a destination; they still record history.
ALTER TABLE alert_rules ALTER COLUMN destination_id DROP NOT NULL;
