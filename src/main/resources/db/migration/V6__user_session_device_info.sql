ALTER TABLE user_sessions
    ADD COLUMN device_label  TEXT,
    ADD COLUMN location_label TEXT,
    ADD COLUMN ip_encrypted  TEXT,
    ADD COLUMN user_agent    TEXT;
