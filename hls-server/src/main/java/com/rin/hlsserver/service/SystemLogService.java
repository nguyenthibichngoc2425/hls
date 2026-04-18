package com.rin.hlsserver.service;

import com.rin.hlsserver.model.SystemLog;
import com.rin.hlsserver.repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SystemLogService {

    private final SystemLogRepository systemLogRepository;

    @Value("${app.server-name:SERVER-UNKNOWN}")
    private String serverName;

    @Value("${server.port:0}")
    private String serverPort;

    @Transactional
    public void save(String eventType,
                     String endpoint,
                     String userEmail,
                     String ipAddress,
                     String port,
                     String message) {
        String normalizedEventType = normalize(eventType, "API_REQUEST");
        String normalizedLevel = "ERROR".equalsIgnoreCase(normalizedEventType) ? "ERROR" : "INFO";

        SystemLog log = SystemLog.builder()
                .serverName(serverName)
                .level(normalizedLevel)
                .eventType(normalizedEventType)
                .endpoint(normalize(endpoint, "-"))
                .userEmail(normalize(userEmail, "anonymous"))
                .ipAddress(normalize(ipAddress, "unknown"))
                .port(normalize(port, serverPort))
                .message(message)
                .build();
        systemLogRepository.save(log);
    }

    @Transactional
    public void save(String eventType,
                     String endpoint,
                     String userEmail,
                     String ipAddress,
                     String message) {
        save(eventType, endpoint, userEmail, ipAddress, serverPort, message);
    }

    @Transactional
    public void save(String eventType,
                     String endpoint,
                     String userEmail,
                     String ipAddress) {
        save(eventType, endpoint, userEmail, ipAddress, serverPort, null);
    }

    @Transactional(readOnly = true)
    public List<SystemLog> getLatestLogs(int limit) {
        return systemLogRepository.findLatestLogs(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<SystemLog> getLatestLogsByCategory(int limit, String category) {
        String normalizedCategory = normalize(category, "ALL").toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalizedCategory)) {
            return getLatestLogs(limit);
        }

        List<String> eventTypes = switch (normalizedCategory) {
            case "LOGIN" -> Arrays.asList("LOGIN_SUCCESS", "LOGIN_FAIL", "LOGOUT");
            case "HLS" -> Arrays.asList("HLS_MASTER", "HLS_PLAYLIST", "HLS_SEGMENT");
            case "ERROR" -> List.of("ERROR");
            default -> new ArrayList<>();
        };

        if (eventTypes.isEmpty()) {
            return getLatestLogs(limit);
        }

        return systemLogRepository.findLatestLogsByEventTypes(eventTypes, PageRequest.of(0, limit));
    }

    @Transactional
    public void clearAll() {
        systemLogRepository.deleteAllInBatch();
    }

    private String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}