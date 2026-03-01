package com.rin.hlsserver.monitor.job;

import com.rin.hlsserver.monitor.store.OnlineWatchingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to clean up timed-out online sessions
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OnlineCleanupJob {
    
    private final OnlineWatchingStore onlineStore;
    
    @Value("${monitor.online.timeoutSeconds:10}")
    private long timeoutSeconds;
    
    /**
     * Run every 4 seconds to remove timed-out sessions
     */
    @Scheduled(fixedDelay = 4000, initialDelay = 4000)
    public void cleanupTimedOutSessions() {
        int removed = onlineStore.removeTimedOutSessions(timeoutSeconds);
        if (removed > 0) {
            log.debug("Đã xóa {} phiên hết hạn", removed);
        }
    }
}
