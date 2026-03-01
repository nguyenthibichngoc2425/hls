package com.rin.hlsserver.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    Long id;
    String email;
    String fullName;
    String status;  // "ACTIVE" hoặc "BANNED"
    Set<String> roles;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
