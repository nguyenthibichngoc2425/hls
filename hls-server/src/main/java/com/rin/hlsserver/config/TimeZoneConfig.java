package com.rin.hlsserver.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Configuration to set JVM timezone to Asia/Ho_Chi_Minh (UTC+7).
 * This ensures all LocalDateTime operations use Vietnam timezone by default.
 * 
 * - Hibernate will use this timezone for all @CreationTimestamp/@UpdateTimestamp
 * - LocalDateTime.now() will return Vietnam time
 * - Database will store times in Vietnam timezone
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void init() {
        // Set default timezone to Asia/Ho_Chi_Minh for entire JVM
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("✅ JVM Default Timezone Set: " + TimeZone.getDefault().getID());
    }
}
