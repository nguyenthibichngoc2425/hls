package com.rin.hlsserver.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "source_video_path")
    private String sourceVideoPath; // Đường dẫn video gốc để xử lý

    @Column(name = "master_playlist_path")
    private String masterPlaylistPath; // Đường dẫn master.m3u8 sau khi xử lý

    private Integer duration; // Thời lượng phim (phút)

    @Column(name = "processing_minutes")
    private Integer processingMinutes; // Số phút muốn xử lý (0 = không xử lý)

    @Column(name = "release_year")
    private Integer releaseYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovieStatus status; // DRAFT, PROCESSING, PUBLISHED, FAILED

    @Column(name = "processing_progress")
    private Integer processingProgress; // 0-100%

    @Column(name = "processing_error")
    private String processingError;

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VideoQuality> videoQualities = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @OneToMany(
            mappedBy = "movie",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<WatchHistory> watchHistories = new ArrayList<>();

    @OneToMany(
            mappedBy = "movie",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<MovieReview> reviews = new ArrayList<>();

    public enum MovieStatus {
        DRAFT,      // Chưa xử lý
        PROCESSING, // Đang xử lý
        PUBLISHED,  // Đã publish
        FAILED      // Xử lý thất bại
    }
}
