package raven.modal.demo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.UserResponse;

import java.util.List;

public class UserApi {

    /**
     * Lấy danh sách tất cả user (chỉ role USER)
     */
    public static ApiResponse<List<UserResponse>> getAllUsers() {
        try {
            TypeReference<ApiResponse<List<UserResponse>>> type =
                    new TypeReference<>() {};
            return Http.get("/users", type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    /**
     * Chặn user
     */
    public static ApiResponse<UserResponse> banUser(Long userId) {
        try {
            TypeReference<ApiResponse<UserResponse>> type =
                    new TypeReference<>() {};
            return Http.put("/users/" + userId + "/ban", null, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    /**
     * Mở chặn user
     */
    public static ApiResponse<UserResponse> unbanUser(Long userId) {
        try {
            TypeReference<ApiResponse<UserResponse>> type =
                    new TypeReference<>() {};
            return Http.put("/users/" + userId + "/unban", null, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }
}
