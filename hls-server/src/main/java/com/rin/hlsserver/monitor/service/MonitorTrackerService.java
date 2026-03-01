package com.rin.hlsserver.monitor.service;

import com.rin.hlsserver.monitor.model.LogEntry;
import com.rin.hlsserver.monitor.model.LogEntry.LogAction;
import com.rin.hlsserver.monitor.store.LogStore;
import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for tracking user activities and online sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorTrackerService {
    
    private final LogStore logStore;
    private final OnlineWatchingStore onlineStore;
    
    /**
     * Track successful login
     */
    public void trackLoginSuccess(String account, String ip) {
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.LOGIN_SUCCESS)
                .account(account)
                .ip(ip)
                .message("Login successful")
                .build();
        logStore.add(entry);
        log.debug("Tracked login success: account={}, ip={}", account, ip);
    }
    
    /**
     * Track failed login
     */
    public void trackLoginFail(String email, String ip, String reason) {
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.LOGIN_FAIL)
                .account(email)
                .ip(ip)
                .message("Login failed: " + reason)
                .build();
        logStore.add(entry);
        log.debug("Tracked login fail: email={}, ip={}, reason={}", email, ip, reason);
    }
    
    /**
     * Track logout
     */
    public void trackLogout(String account, String ip) {
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.LOGOUT)
                .account(account)
                .ip(ip)
                .message("Logged out")
                .build();
        logStore.add(entry);
        System.out.println("[LOGOUT] User " + account + " logged out at " + entry.getTime());
        log.debug("Tracked logout: account={}, ip={}", account, ip);
    }
    
    /**
     * Track master playlist request
     */
    public void trackMasterPlaylist(HttpServletRequest request, String account, String videoId) {
        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);
        String path = request.getRequestURI();
        
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.HLS_MASTER)
                .account(account)
                .ip(ip)
                .videoId(videoId)
                .path(path)
                .message("Master playlist requested")
                .build();
        logStore.add(entry);
        log.debug("Tracked master playlist: account={}, ip={}, videoId={}", account, ip, videoId);
    }
    
    /**
     * Track quality playlist request and update online session
     */
    public void trackPlaylist(HttpServletRequest request, String account, String videoId, String quality) {
        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);
        String path = request.getRequestURI();
        
        // Log the request
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.HLS_PLAYLIST)
                .account(account)
                .ip(ip)
                .videoId(videoId)
                .quality(quality)
                .path(path)
                .message("Playlist requested")
                .build();
        logStore.add(entry);
        
        // Update online session
        onlineStore.upsertSession(account, ip, videoId, quality, userAgent);
        
        log.debug("Tracked playlist: account={}, ip={}, videoId={}, quality={}", account, ip, videoId, quality);
    }
    
    /**
     * Track segment request and update online session
     */
    public void trackSegment(HttpServletRequest request, String account, String videoId, String quality, String segmentName) {
        String ip = extractIp(request);
        String userAgent = extractUserAgent(request);
        String path = request.getRequestURI();
        
        // Log the request
        LogEntry entry = LogEntry.builder()
                .time(Instant.now())
                .action(LogAction.HLS_SEGMENT)
                .account(account)
                .ip(ip)
                .videoId(videoId)
                .quality(quality)
                .path(path)
                .message("Segment: " + segmentName)
                .build();
        logStore.add(entry);
        
        // Update online session
        onlineStore.upsertSession(account, ip, videoId, quality, userAgent);
        
        log.debug("Tracked segment: account={}, ip={}, videoId={}, quality={}, segment={}", 
                account, ip, videoId, quality, segmentName);
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
