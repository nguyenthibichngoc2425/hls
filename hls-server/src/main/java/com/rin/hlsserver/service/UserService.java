package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.response.UserResponse;
import com.rin.hlsserver.exception.BaseException;
import com.rin.hlsserver.model.Role;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.model.UserStatus;
import com.rin.hlsserver.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;

    /**
     * Lấy danh sách tất cả user chỉ có role USER (không bao gồm ADMIN)
     */
    public List<UserResponse> getAllRegularUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .noneMatch(role -> "ADMIN".equals(role.getName())))
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Chặn user
     */
    @Transactional
    public UserResponse banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found with id: " + userId));

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BaseException("User is already banned");
        }

        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
        log.info("User {} ({}) has been banned", user.getEmail(), userId);

        return mapToUserResponse(user);
    }

    /**
     * Mở chặn user
     */
    @Transactional
    public UserResponse unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found with id: " + userId));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BaseException("User is already active");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("User {} ({}) has been unbanned", user.getEmail(), userId);

        return mapToUserResponse(user);
    }

    /**
     * Map User entity sang UserResponse DTO
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
