package com.rin.hlsserver.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_name", nullable = false, length = 50)
    private String serverName;

    @Column(nullable = false, length = 20)
    private String level;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "user_email", length = 100)
    private String userEmail;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "port", length = 10)
    private String port;

    @Column(columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}