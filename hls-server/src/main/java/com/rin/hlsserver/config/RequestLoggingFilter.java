package com.rin.hlsserver.config;

import com.rin.hlsserver.service.SystemLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final SystemLogService systemLogService;

    public RequestLoggingFilter(SystemLogService systemLogService) {
        this.systemLogService = systemLogService;
    }

    @Value("${app.server-name:SERVER-UNKNOWN}")
    private String serverName;

    @Value("${server.port:0}")
    private String serverPort;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            String ip = extractIp(request);
            String userEmail = extractUserEmail(request);
            log.info("[{}][API_REQUEST] {} {} user={} ip={}", serverName, request.getMethod(), uri, userEmail, ip);
            systemLogService.save("API_REQUEST", uri, userEmail, ip, serverPort,
                    request.getMethod() + " " + uri);
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