package raven.modal.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String sourceVideoPath;
    private String masterPlaylistPath;
    private Integer duration;
    private Integer processingMinutes;
    private Integer releaseYear;
    private String status; // DRAFT, PROCESSING, PUBLISHED, FAILED
    private Integer processingProgress; // 0-100
    private String processingError;
    
    private GenreResponse genre;
    private List<VideoQualityResponse> videoQualities;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public String getStatusDisplay() {
        if (status == null) return "DRAFT";
        return switch (status) {
            case "DRAFT" -> "Bản nháp";
            case "PROCESSING" -> "Đang xử lý (" + (processingProgress != null ? processingProgress : 0) + "%)";
            case "PUBLISHED" -> "Đã xuất bản";
            case "FAILED" -> "Thất bại";
            default -> status;
        };
    }
    
    public String getDurationDisplay() {
        if (duration == null) return "N/A";
        int hours = duration / 60;
        int mins = duration % 60;
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }
}
