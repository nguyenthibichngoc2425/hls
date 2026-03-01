package com.rin.hlsserver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    Long id;
    String email;
    String fullName;
    Set<String> roles;
}
