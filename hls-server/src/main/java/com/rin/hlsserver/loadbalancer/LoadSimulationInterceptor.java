package com.rin.hlsserver.loadbalancer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor giả lập tình trạng server overload hoặc bị lỗi
 * Dùng để demo failover mechanism
 */
@Component
public class LoadSimulationInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadSimulationInterceptor.class);
    
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    /**
     * Trước khi xử lý request
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        activeRequests.incrementAndGet();
        int current = activeRequests.get();
        
        String port = getServerPort(request);
        logger.info("[{}] Request started. Active requests: {}", port, current);
        
        // Giả lập random lỗi 20% để demo failover
        if (Math.random() < 0.05) { // 5% random error
            logger.warn("[{}] SIMULATED ERROR: Random failure triggered!", port);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.getWriter().write("{\"error\": \"Service temporarily unavailable (simulated)\"}");
            return false;
        }
        
        // Giả lập overload: nếu > 5 concurrent requests
        if (current > 5) {
            logger.warn("[{}] OVERLOAD: {} active requests > threshold (5)", port, current);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.getWriter().write("{\"error\": \"Server too busy\"}");
            return false;
        }
        
        return true;
    }
    
    /**
     * Sau khi xử lý request
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        activeRequests.decrementAndGet();
        int current = activeRequests.get();
        String port = getServerPort(request);
        
        logger.info("[{}] Request completed. Active requests: {}", port, current);
        
        if (ex != null) {
            logger.error("[{}] Request failed: {}", port, ex.getMessage());
        }
    }
    
    private String getServerPort(HttpServletRequest request) {
        return "PORT:" + request.getServerPort();
    }
    
    public int getActiveRequestCount() {
        return activeRequests.get();
    }
}
