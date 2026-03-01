package com.rin.hlsserver.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents an active watching session for monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchingSession {
    
    private String account;
    private String ip;
    private String videoId;
    private String quality;
    private Instant startedAt;
    private volatile Instant lastSeen;
    private String userAgent;
    
    /**
     * Generate unique key for session identification
     * Format: account|ip|videoId
     */
    public static String generateKey(String account, String ip, String videoId) {
        return account + "|" + ip + "|" + videoId;
    }
    
    /**
     * Get the key for this session
     */
    public String getKey() {
        return generateKey(account, ip, videoId);
    }
}
