package com.rin.loadbalancer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load Balancer Application
 * 
 * Chạy trên port 8080
 * Proxy requests sang 3 backend servers: 8081, 8082, 8083
 * 
 * Round Robin + Failover mechanism
 */
@SpringBootApplication
public class LoadBalancerApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(LoadBalancerApplication.class);
    
    public static void main(String[] args) {
        // In banner
        printBanner();
        
        SpringApplication.run(LoadBalancerApplication.class, args);
        
        logger.info("\n");
        logger.info("╔════════════════════════════════════════════════════════╗");
        logger.info("║   LOAD BALANCER STARTED SUCCESSFULLY                   ║");
        logger.info("║   URL: http://localhost:8080                           ║");
        logger.info("║   Health: http://localhost:8080/lb/health              ║");
        logger.info("║   Status: http://localhost:8080/lb/status              ║");
        logger.info("╚════════════════════════════════════════════════════════╝");
        logger.info("\n");
    }
    
    private static void printBanner() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║                                                        ║");
        System.out.println("║   HLS Platform - LOAD BALANCER                         ║");
        System.out.println("║   Version: 1.0.0                                       ║");
        System.out.println("║                                                        ║");
        System.out.println("║   Backend Servers:                                     ║");
        System.out.println("║   - Backend-1: http://localhost:8081                  ║");
        System.out.println("║   - Backend-2: http://localhost:8082                  ║");
        System.out.println("║   - Backend-3: http://localhost:8083                  ║");
        System.out.println("║                                                        ║");
        System.out.println("║   Policy: Round Robin + Failover                       ║");
        System.out.println("║                                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }
}
