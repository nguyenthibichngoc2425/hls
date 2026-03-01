-- Create favorite_movies table for user's favorite movies
CREATE TABLE IF NOT EXISTS favorite_movies (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorite_movie_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_movie_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_movie_favorite UNIQUE (user_id, movie_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_favorite_movies_user_id ON favorite_movies(user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_movies_created_at ON favorite_movies(created_at DESC);

COMMENT ON TABLE favorite_movies IS 'User favorite movies - tracks which movies users have favorited';
COMMENT ON COLUMN favorite_movies.user_id IS 'User who favorited the movie';
COMMENT ON COLUMN favorite_movies.movie_id IS 'Movie that was favorited';
COMMENT ON COLUMN favorite_movies.created_at IS 'When the movie was added to favorites';
