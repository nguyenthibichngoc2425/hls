package com.rin.hlsserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.rin.hlsserver.service.SystemLogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Slf4j
public class RandomFailureSimulationFilter extends OncePerRequestFilter {

    private final SystemLogService systemLogService;

    public RandomFailureSimulationFilter(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Value("${app.simulation.failureRate:0.0}")
    private double failureRate;

    @Value("${app.server-name:SERVER-UNKNOWN}")
    private String serverName;

    @Value("${server.port:0}")
    private String serverPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (failureRate <= 0.0) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            String ip = extractIp(request);
            String userEmail = extractUserEmail(request);
            String msg = String.format("[%s][SIM] Giả lập lỗi 500 tại %s (rate=%s)", serverName, uri, failureRate);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"message\":\"Giả lập lỗi\",\"server\":\"" + serverName + "\"}");
            systemLogService.save("ERROR", uri, userEmail, ip, serverPort, msg);
            log.warn(msg);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractUserEmail(HttpServletRequest request) {
        String userEmail = request.getParameter("userEmail");
        if (userEmail == null || userEmail.isBlank()) {
            return "anonymous";
        }
        return userEmail;
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}