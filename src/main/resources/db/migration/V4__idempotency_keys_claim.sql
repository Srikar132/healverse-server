-- A key is now claimed (row inserted without a response) before the action runs, so two
-- simultaneous requests with the same key cannot both execute. The response is filled in
-- once the action has succeeded.
ALTER TABLE idempotency_keys ALTER COLUMN response_json DROP NOT NULL;

-- Serves the periodic purge of old rows.
CREATE INDEX idx_idempotency_created_at ON idempotency_keys (created_at);
