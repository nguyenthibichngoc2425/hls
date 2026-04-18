CREATE TABLE IF NOT EXISTS system_logs (
    id BIGSERIAL PRIMARY KEY,
    server_name VARCHAR(50) NOT NULL,
    level VARCHAR(20) NOT NULL,
    event_type VARCHAR(50),
    endpoint VARCHAR(255),
    user_email VARCHAR(100),
    ip_address VARCHAR(50),
    port VARCHAR(10),
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_system_logs_created_at ON system_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_system_logs_server_name ON system_logs(server_name);
CREATE INDEX IF NOT EXISTS idx_system_logs_event_type ON system_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_system_logs_user_email ON system_logs(user_email);