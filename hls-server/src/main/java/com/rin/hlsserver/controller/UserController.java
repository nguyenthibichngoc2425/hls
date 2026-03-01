package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.UserResponse;
import com.rin.hlsserver.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    /**
     * Lấy danh sách tất cả user (chỉ role USER, không bao gồm ADMIN)
     * Chỉ ADMIN mới có quyền truy cập
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        try {
            List<UserResponse> users = userService.getAllRegularUsers();
            return new ApiResponse<>("Get users successfully", users, 200);
        } catch (Exception e) {
            log.error("Error getting users: {}", e.getMessage());
            return new ApiResponse<>(e.getMessage(), null, 500);
        }
    }

    /**
     * Chặn user
     * Chỉ ADMIN mới có quyền
     */
    @PutMapping("/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> banUser(@PathVariable Long id) {
        try {
            UserResponse user = userService.banUser(id);
            return new ApiResponse<>("User banned successfully", user, 200);
        } catch (Exception e) {
            log.error("Error banning user: {}", e.getMessage());
            return new ApiResponse<>(e.getMessage(), null, 400);
        }
    }

    /**
     * Mở chặn user
     * Chỉ ADMIN mới có quyền
     */
    @PutMapping("/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> unbanUser(@PathVariable Long id) {
        try {
            UserResponse user = userService.unbanUser(id);
            return new ApiResponse<>("User unbanned successfully", user, 200);
        } catch (Exception e) {
            log.error("Error unbanning user: {}", e.getMessage());
            return new ApiResponse<>(e.getMessage(), null, 400);
        }
    }
}
