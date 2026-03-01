package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.FavoriteMovieResponse;
import com.rin.hlsserver.service.FavoriteMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Slf4j
public class FavoriteMovieController {

    private final FavoriteMovieService favoriteMovieService;

    /**
     * Thêm phim vào danh sách yêu thích
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FavoriteMovieResponse>> addFavorite(
            @RequestParam Long userId,
            @RequestParam Long movieId) {
        
        FavoriteMovieResponse response = favoriteMovieService.addFavorite(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Xóa phim khỏi danh sách yêu thích
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestParam Long userId,
            @RequestParam Long movieId) {
        
        favoriteMovieService.removeFavorite(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Kiểm tra phim có trong danh sách yêu thích không
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            @RequestParam Long userId,
            @RequestParam Long movieId) {
        
        boolean isFavorite = favoriteMovieService.isFavorite(userId, movieId);
        return ResponseEntity.ok(ApiResponse.success(isFavorite));
    }

    /**
     * Lấy danh sách phim yêu thích của user (có phân trang)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FavoriteMovieResponse>>> getUserFavorites(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<FavoriteMovieResponse> response = favoriteMovieService.getUserFavorites(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lấy danh sách ID phim yêu thích
     */
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<Long>>> getFavoriteIds(@RequestParam Long userId) {
        List<Long> ids = favoriteMovieService.getUserFavoriteMovieIds(userId);
        return ResponseEntity.ok(ApiResponse.success(ids));
    }

    /**
     * Đếm số lượng phim yêu thích của user
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countFavorites(@RequestParam Long userId) {
        long count = favoriteMovieService.countUserFavorites(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
