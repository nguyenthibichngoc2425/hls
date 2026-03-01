package com.rin.hlsserver.monitor.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper để extract thông tin user từ request
 */
@Slf4j
public class JwtHelper {
    
    /**
     * Lấy email từ custom header X-User-Email
     */
    public static String extractEmailFromRequest(HttpServletRequest request) {
        try {
            // Try custom header first
            String email = request.getHeader("X-User-Email");
            if (email != null && !email.isEmpty()) {
                return email;
            }
            
            // Fallback to query parameter
            email = request.getParameter("userEmail");
            if (email != null && !email.isEmpty()) {
                return email;
            }
            
            return "Khách";
        } catch (Exception e) {
            log.debug("Could not extract email from request: {}", e.getMessage());
            return "Khách";
        }
    }
}
