package com.rin.hlsserver.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteMovieResponse {
    private Long id;
    private MovieResponse movie;
    private LocalDateTime createdAt;
}
