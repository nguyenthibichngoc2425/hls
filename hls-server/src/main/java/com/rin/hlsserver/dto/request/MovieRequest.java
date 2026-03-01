package com.rin.hlsserver.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    private String imageUrl;
    
    @NotBlank(message = "Source video path is required")
    private String sourceVideoPath;
    
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer duration; // phút
    
    @NotNull(message = "Processing minutes is required")
    @Min(value = 0, message = "Processing minutes must be >= 0")
    private Integer processingMinutes; // 0 = không xử lý
    
    @Min(value = 1900, message = "Release year must be >= 1900")
    @Max(value = 2100, message = "Release year must be <= 2100")
    private Integer releaseYear;
    
    @NotNull(message = "Genre ID is required")
    private Long genreId;
}
