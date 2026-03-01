package raven.modal.demo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.WatchHistoryResponse;

import java.util.List;

public class WatchHistoryApi {

    public static ApiResponse<WatchHistoryResponse> addOrUpdateWatchHistory(Long userId, Long movieId) {
        try {
            String path = String.format("/watch-history?userId=%d&movieId=%d", userId, movieId);
            TypeReference<ApiResponse<WatchHistoryResponse>> type = new TypeReference<>() {};
            return Http.post(path, null, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Void> removeWatchHistory(Long userId, Long movieId) {
        try {
            String path = String.format("/watch-history?userId=%d&movieId=%d", userId, movieId);
            TypeReference<ApiResponse<Void>> type = new TypeReference<>() {};
            return Http.delete(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<List<WatchHistoryResponse>> getUserWatchHistory(Long userId, int page, int size) {
        try {
            String path = String.format("/watch-history?userId=%d&page=%d&size=%d", userId, page, size);
            TypeReference<ApiResponse<List<WatchHistoryResponse>>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Long> countWatchHistory(Long userId) {
        try {
            String path = String.format("/watch-history/count?userId=%d", userId);
            TypeReference<ApiResponse<Long>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }
}
