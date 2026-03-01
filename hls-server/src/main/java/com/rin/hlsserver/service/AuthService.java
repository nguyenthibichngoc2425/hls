package com.rin.hlsserver.service;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.rin.hlsserver.dto.response.AuthResponse;
import com.rin.hlsserver.exception.BaseException;
import com.rin.hlsserver.model.Role;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.model.UserStatus;
import com.rin.hlsserver.monitor.service.MonitorTrackerService;
import com.rin.hlsserver.repository.RoleRepository;
import com.rin.hlsserver.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    static String DEFAULT_ROLE = "USER";
    static long JWT_EXPIRATION_MS = 86400000; // 1 day
    @Value("${app.jwt.signerKey}")
    @NonFinal
    String JWT_SECRET;
    PasswordEncoder passwordEncoder;


    public AuthResponse registerUser(String email, String password, String fullName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BaseException("Email is already in use");
        }

        Role defaultRole= createDefaultRoleIfNotExist();

        User newUser= User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(defaultRole))
                .build();

        userRepository.save(newUser);

        return buildAuthResponse(newUser);
    }

    public AuthResponse loginUser(String email, String password, HttpServletRequest request, MonitorTrackerService monitorTracker) {
        String ip = extractIp(request);
        
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BaseException("User not found"));

            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                monitorTracker.trackLoginFail(email, ip, "Invalid password");
                throw new BaseException("Invalid credentials");
            }

            // Check if user is banned
            if (user.getStatus() == com.rin.hlsserver.model.UserStatus.BANNED) {
                monitorTracker.trackLoginFail(email, ip, "Account is banned");
                throw new BaseException("Your account has been banned. Please contact administrator.");
            }

            monitorTracker.trackLoginSuccess(email, ip);
            return buildAuthResponse(user);
            
        } catch (BaseException e) {
            if (e.getMessage().contains("not found")) {
                monitorTracker.trackLoginFail(email, ip, "User not found");
            }
            throw e;
        }
    }
    
    public void logoutUser(Long userId, HttpServletRequest request, MonitorTrackerService monitorTracker) {
        String ip = extractIp(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException("User not found"));
        monitorTracker.trackLogout(user.getEmail(), ip);
    }
    
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }


    private Role createDefaultRoleIfNotExist() {
        return roleRepository.findById(DEFAULT_ROLE).orElseGet(() -> {
            Role role= Role.builder().name(DEFAULT_ROLE).build();
            roleRepository.save(role);
            return role;
        });
    }

    private AuthResponse buildAuthResponse(User user ) {

        return AuthResponse.builder()
                .fullName(user.getFullName())
                .email(user.getEmail())
                .id(user.getId())
                .roles(user.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()))
                .build();
    }


//    private String createJWTToken(User user) {
//        JWSHeader jwsHeader= new JWSHeader(JWSAlgorithm.HS512);
//
//        JWTClaimsSet jwtClaimsSet= new JWTClaimsSet.Builder()
//                .subject(user.getId().toString())
//                .issuer("com.rin.hls-server")
//                .issueTime(new Date())
//                .expirationTime(new Date(System.currentTimeMillis() + JWT_EXPIRATION_MS))
//                .jwtID(UUID.randomUUID().toString())
//                .claim("scope", buildScope(user))
//                .build();
//
//        Payload payload= new Payload(jwtClaimsSet.toJSONObject());
//
//        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
//
//        try {
//            jwsObject.sign(new MACSigner(JWT_SECRET.getBytes()));
//            return jwsObject.serialize();
//        } catch (JOSEException e) {
//            log.error("Cannot create token", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private String buildScope(User user) {
//        // ROLE_USER ROLE_ADMIN ...
//        StringJoiner stringJoiner= new StringJoiner(" ");
//        user.getRoles().forEach(role -> stringJoiner.add("ROLE_" + role.getName()));
//        return stringJoiner.toString();
//    }






}
