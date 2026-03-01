package com.rin.hlsserver.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rin.hlsserver.model.Movie;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    private String status;
    private Integer processingProgress;
    private String processingError;
    
    private GenreResponse genre;
    private List<VideoQualityResponse> videoQualities;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;
    
    public static MovieResponse fromEntity(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .imageUrl(movie.getImageUrl())
                .sourceVideoPath(movie.getSourceVideoPath())
                .masterPlaylistPath(movie.getMasterPlaylistPath())
                .duration(movie.getDuration())
                .processingMinutes(movie.getProcessingMinutes())
                .releaseYear(movie.getReleaseYear())
                .status(movie.getStatus().name())
                .processingProgress(movie.getProcessingProgress())
                .processingError(movie.getProcessingError())
                .genre(movie.getGenre() != null ? GenreResponse.fromEntity(movie.getGenre()) : null)
                .videoQualities(movie.getVideoQualities() != null ? 
                        movie.getVideoQualities().stream()
                                .map(VideoQualityResponse::fromEntity)
                                .collect(Collectors.toList()) : null)
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}
