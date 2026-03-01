package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.WatchHistoryResponse;
import com.rin.hlsserver.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/watch-history")
@RequiredArgsConstructor
@Slf4j
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    /**
     * Thêm hoặc cập nhật lịch sử xem phim
     */
    @PostMapping
    public ResponseEntity<ApiResponse<WatchHistoryResponse>> addOrUpdateWatchHistory(
            @RequestParam Long userId,
            @RequestParam Long movieId) {
        
        WatchHistoryResponse response = watchHistoryService.addOrUpdateWatchHistory(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xóa lịch sử xem phim
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeWatchHistory(
            @RequestParam Long userId,
            @RequestParam Long movieId) {
        
        watchHistoryService.removeWatchHistory(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Lấy lịch sử xem phim của user (có phân trang)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<WatchHistoryResponse>>> getUserWatchHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<WatchHistoryResponse> response = watchHistoryService.getUserWatchHistory(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Đếm số lượng lịch sử xem của user
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countWatchHistory(@RequestParam Long userId) {
        long count = watchHistoryService.countUserWatchHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
