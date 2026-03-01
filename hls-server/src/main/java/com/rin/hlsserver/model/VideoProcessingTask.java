package com.rin.hlsserver.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_processing_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoProcessingTask {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;
    
    private Integer progress; // 0-100%
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "processing_duration_minutes")
    private Integer processingDurationMinutes; // Số phút đã xử lý
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TaskStatus {
        PENDING,    // Chờ xử lý
        RUNNING,    // Đang xử lý
        COMPLETED,  // Hoàn thành
        FAILED,     // Thất bại
        CANCELLED   // Đã hủy
    }
}
