-- Create movie_reviews table for user rating and comments
CREATE TABLE IF NOT EXISTS movie_reviews (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movie_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_movie_review_movie FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    CONSTRAINT uk_user_movie_review UNIQUE (user_id, movie_id),
    CONSTRAINT chk_movie_review_rating CHECK (rating BETWEEN 1 AND 5)
);

-- Create indexes for query speed
CREATE INDEX IF NOT EXISTS idx_movie_reviews_movie_id ON movie_reviews(movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_user_id ON movie_reviews(user_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_updated_at ON movie_reviews(updated_at DESC);

COMMENT ON TABLE movie_reviews IS 'User movie reviews with rating and comment';
COMMENT ON COLUMN movie_reviews.rating IS 'Movie rating from 1 to 5';
COMMENT ON COLUMN movie_reviews.comment IS 'User comment for the movie';
