package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.response.MovieResponse;
import com.rin.hlsserver.dto.response.WatchHistoryResponse;
import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.model.WatchHistory;
import com.rin.hlsserver.repository.MovieRepository;
import com.rin.hlsserver.repository.UserRepository;
import com.rin.hlsserver.repository.WatchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    /**
     * Thêm hoặc cập nhật lịch sử xem phim
     * Nếu đã tồn tại thì cập nhật updatedAt, nếu chưa thì tạo mới
     */
    @Transactional
    public WatchHistoryResponse addOrUpdateWatchHistory(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        // Kiểm tra xem đã xem phim này chưa
        Optional<WatchHistory> existingHistory = watchHistoryRepository.findByUserAndMovie(user, movie);

        WatchHistory watchHistory;
        if (existingHistory.isPresent()) {
            // Cập nhật thời gian xem gần nhất
            watchHistory = existingHistory.get();
            watchHistory.setUpdatedAt(LocalDateTime.now());
            log.info("Updated watch history for user {} and movie {}", userId, movieId);
        } else {
            // Tạo mới lịch sử xem
            watchHistory = WatchHistory.builder()
                    .user(user)
                    .movie(movie)
                    .build();
            log.info("Created new watch history for user {} and movie {}", userId, movieId);
        }

        watchHistory = watchHistoryRepository.save(watchHistory);
        return mapToResponse(watchHistory);
    }

    /**
     * Xóa lịch sử xem phim
     */
    @Transactional
    public void removeWatchHistory(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        watchHistoryRepository.deleteByUserAndMovie(user, movie);
        log.info("Removed watch history for user {} and movie {}", userId, movieId);
    }

    /**
     * Lấy lịch sử xem phim của user (có phân trang)
     */
    public List<WatchHistoryResponse> getUserWatchHistory(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<WatchHistory> historyPage = watchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);

        return historyPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số lượng lịch sử xem của user
     */
    public long countUserWatchHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(BaseErrorCode.USER_NOT_FOUND));

        return watchHistoryRepository.countByUserId(userId);
    }

    private WatchHistoryResponse mapToResponse(WatchHistory watchHistory) {
        MovieResponse movieResponse = MovieResponse.fromEntity(watchHistory.getMovie());

        return WatchHistoryResponse.builder()
                .id(watchHistory.getId())
                .movie(movieResponse)
                .createdAt(watchHistory.getCreatedAt())
                .updatedAt(watchHistory.getUpdatedAt())
                .build();
    }
}
