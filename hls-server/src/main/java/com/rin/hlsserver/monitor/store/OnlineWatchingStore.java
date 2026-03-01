package com.rin.hlsserver.monitor.store;

import com.rin.hlsserver.monitor.model.WatchingSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe store for tracking currently watching sessions
 */
@Component
public class OnlineWatchingStore {
    
    private final ConcurrentHashMap<String, WatchingSession> sessions = new ConcurrentHashMap<>();
    
    /**
     * Upsert a watching session
     * Updates lastSeen and quality if session exists, creates new if not
     */
    public void upsertSession(String account, String ip, String videoId, String quality, String userAgent) {
        String key = WatchingSession.generateKey(account, ip, videoId);
        Instant now = Instant.now();
        
        sessions.compute(key, (k, existing) -> {
            if (existing == null) {
                return WatchingSession.builder()
                        .account(account)
                        .ip(ip)
                        .videoId(videoId)
                        .quality(quality)
                        .startedAt(now)
                        .lastSeen(now)
                        .userAgent(userAgent)
                        .build();
            } else {
                existing.setLastSeen(now);
                existing.setQuality(quality);
                existing.setUserAgent(userAgent);
                return existing;
            }
        });
    }
    
    /**
     * Remove session by key
     */
    public void removeSession(String key) {
        sessions.remove(key);
    }
    
    /**
     * Get snapshot of all sessions
     */
    public List<WatchingSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }
    
    /**
     * Remove sessions that have timed out
     * @param timeoutSeconds Number of seconds after which to consider a session timed out
     * @return Number of sessions removed
     */
    public int removeTimedOutSessions(long timeoutSeconds) {
        Instant cutoff = Instant.now().minusSeconds(timeoutSeconds);
        int removed = 0;
        
        List<String> toRemove = new ArrayList<>();
        for (WatchingSession session : sessions.values()) {
            if (session.getLastSeen().isBefore(cutoff)) {
                toRemove.add(session.getKey());
            }
        }
        
        for (String key : toRemove) {
            sessions.remove(key);
            removed++;
        }
        
        return removed;
    }
    
    /**
     * Get current count of online sessions
     */
    public int getOnlineCount() {
        return sessions.size();
    }
}
