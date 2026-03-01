package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.response.FavoriteMovieResponse;
import com.rin.hlsserver.dto.response.MovieResponse;
import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.FavoriteMovie;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.repository.FavoriteMovieRepository;
import com.rin.hlsserver.repository.MovieRepository;
import com.rin.hlsserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteMovieService {

    private final FavoriteMovieRepository favoriteMovieRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    /**
     * Thêm phim vào danh sách yêu thích
     */
    @Transactional
    public FavoriteMovieResponse addFavorite(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        // Kiểm tra đã tồn tại chưa
        if (favoriteMovieRepository.existsByUserAndMovie(user, movie)) {
            throw new AppException(BaseErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        FavoriteMovie favoriteMovie = FavoriteMovie.builder()
                .user(user)
                .movie(movie)
                .build();

        favoriteMovie = favoriteMovieRepository.save(favoriteMovie);
        log.info("Added favorite for user {} and movie {}", userId, movieId);

        return mapToResponse(favoriteMovie);
    }

    /**
     * Xóa phim khỏi danh sách yêu thích
     */
    @Transactional
    public void removeFavorite(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        favoriteMovieRepository.deleteByUserAndMovie(user, movie);
        log.info("Removed favorite for user {} and movie {}", userId, movieId);
    }

    /**
     * Kiểm tra phim có trong danh sách yêu thích không
     */
    public boolean isFavorite(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        return favoriteMovieRepository.existsByUserAndMovie(user, movie);
    }

    /**
     * Lấy danh sách phim yêu thích của user (có phân trang)
     */
    public List<FavoriteMovieResponse> getUserFavorites(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<FavoriteMovie> favoritePage = favoriteMovieRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return favoritePage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách ID phim yêu thích của user
     */
    public List<Long> getUserFavoriteMovieIds(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        return favoriteMovieRepository.findMovieIdsByUserId(userId);
    }

    /**
     * Đếm số lượng phim yêu thích của user
     */
    public long countUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        return favoriteMovieRepository.countByUserId(userId);
    }

    private FavoriteMovieResponse mapToResponse(FavoriteMovie favoriteMovie) {
        MovieResponse movieResponse = MovieResponse.fromEntity(favoriteMovie.getMovie());

        return FavoriteMovieResponse.builder()
                .id(favoriteMovie.getId())
                .movie(movieResponse)
                .createdAt(favoriteMovie.getCreatedAt())
                .build();
    }
}
