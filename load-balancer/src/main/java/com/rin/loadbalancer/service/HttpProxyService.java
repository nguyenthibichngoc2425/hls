package com.rin.loadbalancer.service;

import com.rin.loadbalancer.model.BackendServer;
import com.rin.loadbalancer.strategy.LoadBalancerStrategy;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Service proxy HTTP requests từ load balancer sang backend servers
 */
@Service
public class HttpProxyService {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyService.class);
    
    private final LoadBalancerStrategy strategy;
    private final RestTemplate restTemplate;
    
    public HttpProxyService(LoadBalancerStrategy strategy) {
        this.strategy = strategy;
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Proxy request đến backend servers với cơ chế retry
     */
    public ResponseEntity<String> proxyRequest(
            List<BackendServer> servers,
            String method,
            String path,
            String body,
            HttpHeaders headers) {
        
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < servers.size()) {
            BackendServer server = strategy.selectServer(servers);
            
            if (server == null) {
                logger.error("[LB] No healthy server available after {} retries", retryCount);
                return ResponseEntity.status(503).body("{\"error\": \"All backends are down\"}");
            }
            
            try {
                String targetUrl = server.getUrl() + path;
                logger.info("[LB] Attempt {} - Forward {} request to {} - URL: {}", 
                    retryCount + 1, method, server.id, targetUrl);
                
                ResponseEntity<String> response = forwardRequest(method, targetUrl, body, headers);
                
                // Request thành công
                logger.info("[LB] ✓ {} responded with status {}", server.id, response.getStatusCodeValue());
                return response;
                
            } catch (Exception e) {
                logger.warn("[LB] ✗ {} connection failed: {}", server.id, e.getMessage());
                lastException = e;
                retryCount++;
            }
        }
        
        logger.error("[LB] All {} servers failed! Last error: {}", servers.size(), 
            lastException != null ? lastException.getMessage() : "Unknown");
        return ResponseEntity.status(503).body("{\"error\": \"Service Unavailable - All backends failed\"}");
    }
    
    /**
     * Thực tế forward request
     */
    private ResponseEntity<String> forwardRequest(
            String method,
            String url,
            String body,
            HttpHeaders headers) {
        
        try {
            // Tạo copy của headers, bỏ Host header để tránh conflict
            HttpHeaders forwardHeaders = new HttpHeaders();
            headers.forEach((key, values) -> {
                if (!key.equalsIgnoreCase("Host")) {
                    forwardHeaders.addAll(key, values);
                }
            });
            
            switch (method.toUpperCase()) {
                case "GET":
                    return restTemplate.getForEntity(url, String.class);
                case "POST":
                    return restTemplate.postForEntity(url, body, String.class);
                case "PUT":
                    restTemplate.put(url, body);
                    return ResponseEntity.ok(body);
                case "DELETE":
                    restTemplate.delete(url);
                    return ResponseEntity.ok().build();
                default:
                    return ResponseEntity.status(405).body("Method not allowed");
            }
        } catch (HttpServerErrorException | HttpClientErrorException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
