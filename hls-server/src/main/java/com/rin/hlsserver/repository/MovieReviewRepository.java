package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.MovieReview;
import com.rin.hlsserver.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieReviewRepository extends JpaRepository<MovieReview, Long> {

    Optional<MovieReview> findByUserIdAndMovieId(Long userId, Long movieId);

    Optional<MovieReview> findByUserAndMovieId(User user, Long movieId);

    @Query("SELECT mr FROM MovieReview mr JOIN FETCH mr.user WHERE mr.movie.id = :movieId ORDER BY mr.updatedAt DESC")
    List<MovieReview> findByMovieIdOrderByUpdatedAtDesc(Long movieId, Pageable pageable);

    long countByMovieId(Long movieId);

    @Query("SELECT AVG(mr.rating) FROM MovieReview mr WHERE mr.movie.id = :movieId")
    Optional<Double> calculateAverageRatingByMovieId(Long movieId);
}
