package com.rin.hlsserver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreRequest {
    @NotBlank(message = "Genre ID không được để trống")
    @Size(max = 50, message = "Genre ID không được quá 50 ký tự")
    private String genreId; // action, drama, etc.

    @NotBlank(message = "Tên thể loại không được để trống")
    @Size(max = 100, message = "Tên thể loại không được quá 100 ký tự")
    private String name; // Hành Động, Chính Kịch, etc.

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;
}
