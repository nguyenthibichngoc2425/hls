package com.rin.hlsserver.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "video_qualities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoQuality {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Quality quality;
    
    @Column(nullable = false)
    private String playlistPath; // Đường dẫn file .m3u8
    
    @Column(nullable = false)
    private String segmentsPath; // Thư mục chứa các segment .ts
    
    private Integer bitrate; // kbps
    
    private String resolution; // 640x360, 1280x720
    
    private Long fileSize; // bytes
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    public enum Quality {
        Q360P("360p", 640, 360, 800),   // 800 kbps
        Q720P("720p", 1280, 720, 2500); // 2500 kbps
        
        private final String label;
        private final int width;
        private final int height;
        private final int bitrate;
        
        Quality(String label, int width, int height, int bitrate) {
            this.label = label;
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
        }
        
        public String getLabel() {
            return label;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public int getBitrate() {
            return bitrate;
        }
        
        public String getResolution() {
            return width + "x" + height;
        }
    }
}
