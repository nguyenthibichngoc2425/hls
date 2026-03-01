//package com.rin.hlsserver.config;
//
//
//import com.rin.hlsserver.dto.response.ApiResponse;
//import com.rin.hlsserver.exception.BaseErrorCode;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.MediaType;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import tools.jackson.databind.ObjectMapper;
//
//import java.awt.*;
//import java.io.IOException;
//
//public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
//
//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
//            throws IOException {
//        BaseErrorCode errorCode= BaseErrorCode.UNAUTHENTICATED;
//
//        response.setStatus(errorCode.getStatus().value());
//
//        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//
//        ApiResponse<?> apiResponse = ApiResponse.builder()
//                .code(errorCode.getCode())
//                .message(errorCode.getMessage())
//                .build();
//
//        //Convert Object to Json
//        ObjectMapper objectMapper= new ObjectMapper();
//
//
//        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
//        response.flushBuffer();
//    }
//}