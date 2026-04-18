ALTER TABLE system_logs
    ADD COLUMN IF NOT EXISTS event_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS endpoint VARCHAR(255),
    ADD COLUMN IF NOT EXISTS user_email VARCHAR(100),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50),
    ADD COLUMN IF NOT EXISTS port VARCHAR(10);

CREATE INDEX IF NOT EXISTS idx_system_logs_event_type ON system_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_system_logs_endpoint ON system_logs(endpoint);
CREATE INDEX IF NOT EXISTS idx_system_logs_user_email ON system_logs(user_email);
