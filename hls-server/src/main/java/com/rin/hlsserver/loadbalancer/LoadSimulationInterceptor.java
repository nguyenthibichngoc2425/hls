package com.rin.hlsserver.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor giả lập capacity theo số viewer active trên từng server.
 *
 * Logic:
 * - Chỉ áp dụng cho luồng HLS (/api/hls/**)
 * - Mỗi viewer được nhận diện bởi userEmail (hoặc IP fallback)
 * - Viewer được giữ bằng in-memory TTL lease
 * - Mỗi server chỉ cho phép tối đa N viewer active (mặc định 2)
 */
@Component
public class LoadSimulationInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadSimulationInterceptor.class);

    private final ConcurrentHashMap<String, Long> activeViewers = new ConcurrentHashMap<>();

    @Value("${app.simulation.viewer.max-active-per-server:2}")
    private int maxActiveViewers;

    @Value("${app.simulation.viewer.ttl-seconds:20}")
    private int viewerTtlSeconds;

    @Value("${app.server-name:SERVER-UNKNOWN}")
    private String serverName;
    
    /**
     * Trước khi xử lý request
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/hls/")) {
            return true;
        }

        evictExpiredViewers();

        String viewerKey = buildViewerKey(request);
        long now = System.currentTimeMillis();
        boolean isExistingViewer = activeViewers.containsKey(viewerKey);

        if (!isExistingViewer && activeViewers.size() >= maxActiveViewers) {
            logger.warn("[{}] REJECT viewer={} active={} limit={} uri={}",
                    serverName, viewerKey, activeViewers.size(), maxActiveViewers, uri);

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Server viewer capacity reached\",\"server\":\""
                    + serverName + "\",\"active\":" + activeViewers.size() + ",\"limit\":" + maxActiveViewers + "}");
            return false;
        }

        activeViewers.put(viewerKey, now);
        if (isExistingViewer) {
            logger.debug("[{}] REFRESH viewer={} active={} uri={}", serverName, viewerKey, activeViewers.size(), uri);
        } else {
            logger.info("[{}] ACCEPT viewer={} active={}/{} uri={}",
                    serverName, viewerKey, activeViewers.size(), maxActiveViewers, uri);
        }

        return true;
    }
    
    @Scheduled(fixedDelayString = "${app.simulation.viewer.cleanup-interval-ms:5000}")
    public void cleanupExpiredViewers() {
        evictExpiredViewers();
    }

    private void evictExpiredViewers() {
        long now = System.currentTimeMillis();
        long ttlMillis = viewerTtlSeconds * 1000L;

        Iterator<Map.Entry<String, Long>> iterator = activeViewers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > ttlMillis) {
                logger.info("[{}] EVICT viewer={} idleMs={}", serverName, entry.getKey(), now - entry.getValue());
                iterator.remove();
            }
        }
    }

    private String buildViewerKey(HttpServletRequest request) {
        String userEmail = normalize(request.getParameter("userEmail"));

        if (userEmail.isEmpty()) {
            userEmail = "ip:" + extractIp(request);
        }

        return userEmail;
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    public int getActiveViewerCount() {
        evictExpiredViewers();
        return activeViewers.size();
    }

    public int getMaxActiveViewers() {
        return maxActiveViewers;
    }

    public int getViewerTtlSeconds() {
        return viewerTtlSeconds;
    }

    public List<ViewerLease> getActiveViewerSnapshot() {
        evictExpiredViewers();
        List<ViewerLease> snapshot = new ArrayList<>();
        for (Map.Entry<String, Long> entry : activeViewers.entrySet()) {
            snapshot.add(new ViewerLease(entry.getKey(), entry.getValue()));
        }
        snapshot.sort((a, b) -> Long.compare(b.lastSeenEpochMillis(), a.lastSeenEpochMillis()));
        return snapshot;
    }

    public record ViewerLease(String viewerId, long lastSeenEpochMillis) {
    }
}
