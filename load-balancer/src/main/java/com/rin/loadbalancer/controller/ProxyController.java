package com.rin.loadbalancer.controller;

import com.rin.loadbalancer.model.BackendServer;
import com.rin.loadbalancer.service.HealthCheckService;
import com.rin.loadbalancer.service.HttpProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller nhận requests từ client và proxy sang backend servers
 */
@RestController
@RequestMapping("")
public class ProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProxyController.class);
    
    private final HttpProxyService httpProxyService;
    private final HealthCheckService healthCheckService;
    private final List<BackendServer> backendServers;
    
    public ProxyController(HttpProxyService httpProxyService, HealthCheckService healthCheckService) {
        this.httpProxyService = httpProxyService;
        this.healthCheckService = healthCheckService;
        
        // Định nghĩa các backend servers
        this.backendServers = List.of(
            new BackendServer("Backend-1", "localhost", 8081, "http://localhost:8081", true, 0, 0),
            new BackendServer("Backend-2", "localhost", 8082, "http://localhost:8082", true, 0, 0),
            new BackendServer("Backend-3", "localhost", 8083, "http://localhost:8083", true, 0, 0)
        );
        
        logger.info("[LB] Load Balancer initialized with {} backend servers", backendServers.size());
        logServersConfig();
    }
    
    /**
     * POST requests
     */
    @PostMapping("/**")
    public ResponseEntity<String> handlePost(
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) String body,
            @RequestParam(defaultValue = "") String path) {
        
        String fullPath = extractPath();
        logger.info("[LB] >>> POST {} from client", fullPath);
        
        // Health check trước
        healthCheckService.checkAllServersHealth(backendServers);
        
        return httpProxyService.proxyRequest(backendServers, "POST", fullPath, body, headers);
    }
    
    /**
     * GET requests
     */
    @GetMapping("/**")
    public ResponseEntity<String> handleGet(
            @RequestHeader HttpHeaders headers,
            @RequestParam(defaultValue = "") String path) {
        
        String fullPath = extractPath();
        logger.info("[LB] >>> GET {} from client", fullPath);
        
        // Health check trước
        healthCheckService.checkAllServersHealth(backendServers);
        
        return httpProxyService.proxyRequest(backendServers, "GET", fullPath, "", headers);
    }
    
    /**
     * PUT requests
     */
    @PutMapping("/**")
    public ResponseEntity<String> handlePut(
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) String body) {
        
        String fullPath = extractPath();
        logger.info("[LB] >>> PUT {} from client", fullPath);
        
        healthCheckService.checkAllServersHealth(backendServers);
        
        return httpProxyService.proxyRequest(backendServers, "PUT", fullPath, body, headers);
    }
    
    /**
     * DELETE requests
     */
    @DeleteMapping("/**")
    public ResponseEntity<String> handleDelete(
            @RequestHeader HttpHeaders headers) {
        
        String fullPath = extractPath();
        logger.info("[LB] >>> DELETE {} from client", fullPath);
        
        healthCheckService.checkAllServersHealth(backendServers);
        
        return httpProxyService.proxyRequest(backendServers, "DELETE", fullPath, "", headers);
    }
    
    /**
     * Health endpoint của load balancer
     */
    @GetMapping("/lb/health")
    public ResponseEntity<String> loadBalancerHealth() {
        healthCheckService.checkAllServersHealth(backendServers);
        
        int aliveCount = (int) backendServers.stream()
            .filter(BackendServer::isAlive)
            .count();
        
        String response = String.format(
            "{\"status\": \"ok\", \"alive_servers\": %d, \"total_servers\": %d}",
            aliveCount, backendServers.size()
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Status endpoint
     */
    @GetMapping("/lb/status")
    public ResponseEntity<String> loadBalancerStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"load_balancer\": \"Active\",\n");
        sb.append("  \"backend_servers\": [\n");
        
        for (int i = 0; i < backendServers.size(); i++) {
            BackendServer server = backendServers.get(i);
            sb.append(String.format(
                "    {\"id\": \"%s\", \"status\": \"%s\", \"port\": %d}%s\n",
                server.id,
                server.isAlive() ? "ALIVE" : "DOWN",
                server.port,
                i < backendServers.size() - 1 ? "," : ""
            ));
        }
        
        sb.append("  ]\n");
        sb.append("}");
        
        return ResponseEntity.ok(sb.toString());
    }
    
    private String extractPath() {
        String path = "/";
        // Lấy đường dẫn từ request
        try {
            var request = org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes().resolveReference("org.springframework.web.servlet.HandlerMapping.pathWithinHandlerMapping");
            if (request != null) {
                path = request.toString();
            }
        } catch (Exception ignore) {}
        return path;
    }
    
    private void logServersConfig() {
        logger.info("[LB] ========== BACKEND SERVERS CONFIG ==========");
        for (BackendServer server : backendServers) {
            logger.info("[LB] {} → {}", server.id, server.getUrl());
        }
        logger.info("[LB] =============================================");
    }
}
