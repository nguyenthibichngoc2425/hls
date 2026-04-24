package raven.modal.demo.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieReviewRequest {

    private Long userId;
    private Integer rating;
    private String comment;
}
