package com.rin.hlsserver.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRatingSummaryResponse {

    private Long movieId;
    private BigDecimal averageRating;
    private Long ratingCount;
}
