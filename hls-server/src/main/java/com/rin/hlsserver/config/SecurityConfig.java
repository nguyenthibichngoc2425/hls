package com.rin.hlsserver.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    //    @Value("${app.jwt.signerKey}")
//    private String JWT_SECRET;
//
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {

        httpSecurity.authorizeHttpRequests(request ->
                        request.anyRequest().permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for simplicity
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable()) // Allow embedding for video player
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self' 'unsafe-inline' 'unsafe-eval' *"))
                )
        ;

        return httpSecurity.build();
    }

//
//    @Bean
//    public JwtDecoder jwtDecoder() {
//        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(), "HmacSHA512");
//        return NimbusJwtDecoder.withSecretKey(key).build();
//    }
//
//    @Bean
//    JwtAuthenticationConverter jwtAuthenticationConverter() {
//        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");
//
//        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
//
//        return jwtAuthenticationConverter;
//    }


}
