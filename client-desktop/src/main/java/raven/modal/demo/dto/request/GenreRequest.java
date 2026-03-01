package raven.modal.demo.dto.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequest {
    private String genreId; // action, drama, etc.
    private String name; // Hành Động, Chính Kịch, etc.
    private String description;
}
