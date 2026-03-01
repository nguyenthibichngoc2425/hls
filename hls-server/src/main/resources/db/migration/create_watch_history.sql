-- Create watch_history table for tracking user's movie viewing history
CREATE TABLE IF NOT EXISTS watch_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_watch_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_watch_history_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_movie_watch UNIQUE (user_id, movie_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_watch_history_user_id ON watch_history(user_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_updated_at ON watch_history(updated_at DESC);

COMMENT ON TABLE watch_history IS 'User watch history - tracks when users view movies';
COMMENT ON COLUMN watch_history.user_id IS 'User who watched the movie';
COMMENT ON COLUMN watch_history.movie_id IS 'Movie that was watched';
COMMENT ON COLUMN watch_history.created_at IS 'When the movie was first watched';
COMMENT ON COLUMN watch_history.updated_at IS 'When the movie was last watched (for sorting recent)';
