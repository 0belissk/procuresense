CREATE TABLE IF NOT EXISTS purchase_load_audit (
    org_id VARCHAR(100) PRIMARY KEY,
    last_loaded_at TIMESTAMPTZ NOT NULL
);
