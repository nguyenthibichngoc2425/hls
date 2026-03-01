package com.rin.hlsserver.monitor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a log entry for monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    
    private Instant time;
    private LogAction action;
    private String account;
    private String ip;
    private String videoId;
    private String quality;
    private String path;
    private String message;
    
    /**
     * Log action types
     */
    public enum LogAction {
        LOGIN_SUCCESS,
        LOGIN_FAIL,
        LOGOUT,
        HLS_MASTER,
        HLS_PLAYLIST,
        HLS_SEGMENT
    }
}
