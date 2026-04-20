package com.rin.hlsserver.loadbalancer;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Đăng ký LoadSimulationInterceptor với Spring
 */
@Configuration
public class LoadBalancerInterceptorConfig implements WebMvcConfigurer {
    
    private final LoadSimulationInterceptor loadSimulationInterceptor;
    
    public LoadBalancerInterceptorConfig(LoadSimulationInterceptor loadSimulationInterceptor) {
        this.loadSimulationInterceptor = loadSimulationInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loadSimulationInterceptor)
            .addPathPatterns("/api/**", "/hls/**")
            .order(1);
    }
}
