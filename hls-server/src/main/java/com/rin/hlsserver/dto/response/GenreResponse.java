package com.rin.hlsserver.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rin.hlsserver.model.Genre;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreResponse {
    private Long id;
    private String genreId;
    private String name;
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime updatedAt;
    
    public static GenreResponse fromEntity(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .genreId(genre.getGenreId())
                .name(genre.getName())
                .description(genre.getDescription())
                .createdAt(genre.getCreatedAt())
                .updatedAt(genre.getUpdatedAt())
                .build();
    }
}
