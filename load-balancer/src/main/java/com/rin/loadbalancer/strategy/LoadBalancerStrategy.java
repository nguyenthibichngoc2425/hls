package com.rin.loadbalancer.strategy;

import com.rin.loadbalancer.model.BackendServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load Balancer Strategy: Round Robin + Failover
 * 
 * Các hành động:
 * 1. Chọn server theo Round Robin
 * 2. Nếu server DOWN hoặc lỗi → cố gắng server khác
 * 3. Tối đa retry = số lượng servers
 */
@Component
public class LoadBalancerStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerStrategy.class);
    
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    
    /**
     * Chọn server tiếp theo theo Round Robin
     */
    public BackendServer selectServer(List<BackendServer> servers) {
        return selectServerWithRetry(servers, 0);
    }
    
    /**
     * Chọn server với cơ chế retry (failover)
     */
    private BackendServer selectServerWithRetry(List<BackendServer> servers, int retryCount) {
        if (servers.isEmpty()) {
            logger.error("[LB] NO SERVERS AVAILABLE!");
            return null;
        }
        
        if (retryCount >= servers.size()) {
            logger.error("[LB] ALL SERVERS FAILED! No more retries available.");
            return null;
        }
        
        // Round Robin: lấy index tiếp theo
        int nextIndex = currentIndex.getAndUpdate(i -> (i + 1) % servers.size());
        BackendServer selectedServer = servers.get(nextIndex);
        
        // Nếu server đã chọn còn sống → dùng nó
        if (selectedServer.isAlive()) {
            logger.info("[LB] Round {} - Selected: {} (ALIVE)", retryCount, selectedServer.id);
            return selectedServer;
        }
        
        // Nếu server DOWN → thử server tiếp theo
        logger.warn("[LB] Round {} - {} is DOWN, trying next server...", retryCount, selectedServer.id);
        return selectServerWithRetry(servers, retryCount + 1);
    }
    
    /**
     * Reset index (dùng cho testing)
     */
    public void resetIndex() {
        currentIndex.set(0);
    }
}
