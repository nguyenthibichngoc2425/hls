package com.rin.loadbalancer.service;

import com.rin.loadbalancer.model.BackendServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service kiểm tra sức khỏe của các backend servers
 */
@Service
public class HealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);
    private static final int HEALTH_CHECK_INTERVAL = 3; // giây
    private static final int TIMEOUT_THRESHOLD = 5000; // ms
    
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public HealthCheckService() {
        this.restTemplate = new RestTemplate();
        startHealthCheckScheduler();
    }
    
    /**
     * Bắt đầu lịch trình health check định kỳ
     */
    private void startHealthCheckScheduler() {
        scheduler.scheduleAtFixedRate(
            () -> logger.debug("[LB] Health check cycle started"),
            HEALTH_CHECK_INTERVAL,
            HEALTH_CHECK_INTERVAL,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Kiểm tra sức khỏe của 1 server
     */
    public boolean checkServerHealth(BackendServer server) {
        try {
            long startTime = System.currentTimeMillis();
            String healthUrl = server.getUrl() + "/actuator/health";
            
            // Thử gọi health endpoint
            var response = restTemplate.getForEntity(healthUrl, String.class);
            long elapsedTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("[LB] ✓ {} - HEALTHY (response time: {}ms)", server.id, elapsedTime);
                server.setAlive(true);
                server.setFailureCount(0);
                return true;
            } else {
                logger.warn("[LB] ✗ {} - UNHEALTHY (status: {})", server.id, response.getStatusCode());
                server.setAlive(false);
                server.setFailureCount(server.failureCount + 1);
                return false;
            }
        } catch (Exception e) {
            logger.warn("[LB] ✗ {} - DOWN (reason: {})", server.id, e.getMessage());
            server.setAlive(false);
            server.setFailureCount(server.failureCount + 1);
            return false;
        }
    }
    
    /**
     * Kiểm tra sức khỏe của tất cả servers
     */
    public void checkAllServersHealth(List<BackendServer> servers) {
        for (BackendServer server : servers) {
            checkServerHealth(server);
        }
        
        // Hiển thị trạng thái tất cả servers
        logServersStatus(servers);
    }
    
    private void logServersStatus(List<BackendServer> servers) {
        logger.info("[LB] ========== SERVERS STATUS ==========");
        int aliveCount = 0;
        for (BackendServer server : servers) {
            String status = server.isAlive() ? "✓ ALIVE" : "✗ DOWN";
            logger.info("[LB] {} - {} (failures: {})", server.id, status, server.failureCount);
            if (server.isAlive()) aliveCount++;
        }
        logger.info("[LB] Total: {} alive / {} total", aliveCount, servers.size());
        logger.info("[LB] ====================================");
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
