package raven.modal.demo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.FavoriteMovieResponse;

import java.util.List;

public class FavoriteApi {

    public static ApiResponse<FavoriteMovieResponse> addFavorite(Long userId, Long movieId) {
        try {
            String path = String.format("/favorites?userId=%d&movieId=%d", userId, movieId);
            TypeReference<ApiResponse<FavoriteMovieResponse>> type = new TypeReference<>() {};
            return Http.post(path, null, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Void> removeFavorite(Long userId, Long movieId) {
        try {
            String path = String.format("/favorites?userId=%d&movieId=%d", userId, movieId);
            TypeReference<ApiResponse<Void>> type = new TypeReference<>() {};
            return Http.delete(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Boolean> checkFavorite(Long userId, Long movieId) {
        try {
            String path = String.format("/favorites/check?userId=%d&movieId=%d", userId, movieId);
            TypeReference<ApiResponse<Boolean>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<List<FavoriteMovieResponse>> getUserFavorites(Long userId, int page, int size) {
        try {
            String path = String.format("/favorites?userId=%d&page=%d&size=%d", userId, page, size);
            TypeReference<ApiResponse<List<FavoriteMovieResponse>>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<List<Long>> getFavoriteIds(Long userId) {
        try {
            String path = String.format("/favorites/ids?userId=%d", userId);
            TypeReference<ApiResponse<List<Long>>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Long> countFavorites(Long userId) {
        try {
            String path = String.format("/favorites/count?userId=%d", userId);
            TypeReference<ApiResponse<Long>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }
}
