ALTER TABLE user_sessions
    DROP COLUMN IF EXISTS device_label,
    DROP COLUMN IF EXISTS location_label;
