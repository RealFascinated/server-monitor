-- A firing alert can be silenced until its value gets worse than silenced_value, e.g. a drive that reports
-- a fixed number of CRC errors from a bad cable.
ALTER TABLE alert_events ADD COLUMN silenced_at TIMESTAMPTZ;
ALTER TABLE alert_events ADD COLUMN silenced_value DOUBLE PRECISION;
