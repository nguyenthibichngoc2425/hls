package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.request.MovieReviewRequest;
import com.rin.hlsserver.dto.response.MovieRatingSummaryResponse;
import com.rin.hlsserver.dto.response.MovieReviewResponse;
import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.MovieReview;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.repository.MovieRepository;
import com.rin.hlsserver.repository.MovieReviewRepository;
import com.rin.hlsserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieReviewService {

    private final MovieReviewRepository movieReviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @Transactional
    public MovieReviewResponse upsertReview(Long movieId, MovieReviewRequest request) {
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new AppException(BaseErrorCode.INVALID_RATING);
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        MovieReview review = movieReviewRepository.findByUserAndMovieId(user, movieId)
                .map(existing -> {
                    existing.setRating(request.getRating());
                    existing.setComment(normalizeComment(request.getComment()));
                    return existing;
                })
                .orElseGet(() -> MovieReview.builder()
                        .user(user)
                        .movie(movie)
                        .rating(request.getRating())
                        .comment(normalizeComment(request.getComment()))
                        .build());

        MovieReview saved = movieReviewRepository.save(review);
        log.info("Upsert review for movie {} by user {}", movieId, request.getUserId());
        return mapToResponse(saved);
    }

    public List<MovieReviewResponse> getMovieReviews(Long movieId, int page, int size) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return movieReviewRepository.findByMovieIdOrderByUpdatedAtDesc(movieId, pageable)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public MovieRatingSummaryResponse getMovieRatingSummary(Long movieId) {
        movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        long ratingCount = movieReviewRepository.countByMovieId(movieId);
        BigDecimal averageRating = movieReviewRepository.calculateAverageRatingByMovieId(movieId)
                .map(value -> BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP))
                .orElse(null);

        return MovieRatingSummaryResponse.builder()
                .movieId(movieId)
                .averageRating(averageRating)
                .ratingCount(ratingCount)
                .build();
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private MovieReviewResponse mapToResponse(MovieReview review) {
        return MovieReviewResponse.builder()
                .id(review.getId())
                .movieId(review.getMovie().getId())
                .userId(review.getUser().getId())
                .userFullName(review.getUser().getFullName())
                .userEmail(review.getUser().getEmail())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
