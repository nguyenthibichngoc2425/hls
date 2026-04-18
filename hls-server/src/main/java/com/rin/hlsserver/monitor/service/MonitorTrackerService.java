package com.rin.hlsserver.monitor.service;

import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import com.rin.hlsserver.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for tracking user activities and online sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorTrackerService {

    private final SystemLogService systemLogService;
    private final OnlineWatchingStore onlineStore;

    @Value("${app.server-name:SERVER-UNKNOWN}")
    private String serverName;

    @Value("${server.port:0}")
    private String serverPort;
    
    /**
     * Track successful login
     */
    public void trackLoginSuccess(String account, String ip) {
        systemLogService.save("LOGIN_SUCCESS", "/api/auth/login", account, ip, serverPort,
                "Dang nhap thanh cong");
        log.info("[{}][AUTH] Đăng nhập thành công user={} ip={}", serverName, account, ip);
    }
    
    /**
     * Track failed login
     */
    public void trackLoginFail(String email, String ip, String reason) {
        systemLogService.save("LOGIN_FAIL", "/api/auth/login", email, ip, serverPort, reason);
        log.warn("[{}][AUTH] Đăng nhập thất bại user={} ip={} reason={}", serverName, email, ip, reason);
    }
    
    /**
     * Track logout
     */
    public void trackLogout(String account, String ip) {
        systemLogService.save("LOGOUT", "/api/auth/logout", account, ip, serverPort, "Dang xuat");
        log.info("[{}][AUTH] Đăng xuất user={} ip={}", serverName, account, ip);
    }
    
    /**
     * Track master playlist request
     */
    public void trackMasterPlaylist(HttpServletRequest request, String account, String videoId) {
        String ip = extractIp(request);
        String path = request.getRequestURI();
        
        systemLogService.save("HLS_MASTER", path, account, ip, serverPort,
                "movieId=" + videoId);
        log.info("[{}][HLS] Request master.m3u8 movieId={} account={} ip={}", serverName, videoId, account, ip);
    }
    
    /**
     * Track quality playlist request and update online session
     */
    public void trackPlaylist(HttpServletRequest request, String account, String videoId, String quality) {
        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);
        String path = request.getRequestURI();
        
        systemLogService.save("HLS_PLAYLIST", path, account, ip, serverPort,
            "movieId=" + videoId + ", quality=" + quality);
        
        // Update online session
        onlineStore.upsertSession(account, ip, videoId, quality, userAgent);
        
        log.info("[{}][HLS] Trả playlist movieId={} quality={} account={} ip={}", serverName, videoId, quality, account, ip);
    }
    
    /**
     * Track segment request and update online session
     */
    public void trackSegment(HttpServletRequest request, String account, String videoId, String quality, String segmentName) {
        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);
        String path = request.getRequestURI();
        
        systemLogService.save("HLS_SEGMENT", path, account, ip, serverPort,
            "movieId=" + videoId + ", quality=" + quality + ", segment=" + segmentName);
        
        // Update online session
        onlineStore.upsertSession(account, ip, videoId, quality, userAgent);
        
        log.info("[{}][HLS] Trả segment {} movieId={} quality={} account={} ip={}", 
            serverName, segmentName, videoId, quality, account, ip);
    }
    
    /**
     * Extract IP address from request
     * Checks X-Forwarded-For header for proxy support
     */
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs in X-Forwarded-For, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
    
    /**
     * Extract User-Agent from request
     */
    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
}
