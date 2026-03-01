package raven.modal.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieRequest {
    private String title;
    private String description;
    private String imageUrl;
    private String sourceVideoPath;
    private Integer duration; // phút
    private Integer processingMinutes; // 0 = không xử lý
    private Integer releaseYear;
    private Long genreId;
}
