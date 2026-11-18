-- The zone that decides where a user's day starts and ends. Logs stay stored as UTC.
ALTER TABLE users ADD COLUMN time_zone character varying(64) NOT NULL DEFAULT 'UTC';
