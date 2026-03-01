package raven.modal.demo.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistoryResponse {
    private Long id;
    private MovieResponse movie;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
