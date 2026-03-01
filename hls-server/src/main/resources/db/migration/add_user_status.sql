-- Add status column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Add check constraint to ensure only ACTIVE or BANNED values
ALTER TABLE users ADD CONSTRAINT check_user_status 
    CHECK (status IN ('ACTIVE', 'BANNED'));

-- Update existing users to ACTIVE if null
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
