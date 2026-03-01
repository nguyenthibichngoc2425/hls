package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.request.GenreRequest;
import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.GenreResponse;
import com.rin.hlsserver.service.GenreService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GenreController {

    GenreService genreService;

    @GetMapping
    public ApiResponse<List<GenreResponse>> getAllGenres() {
        return ApiResponse.success(genreService.getAllGenres());
    }

    @GetMapping("/{id}")
    public ApiResponse<GenreResponse> getGenreById(@PathVariable Long id) {
        return ApiResponse.success(genreService.getGenreById(id));
    }

    @PostMapping
    public ApiResponse<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        return ApiResponse.success(genreService.createGenre(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GenreResponse> updateGenre(
            @PathVariable Long id,
            @Valid @RequestBody GenreRequest request) {
        return ApiResponse.success(genreService.updateGenre(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGenre(@PathVariable Long id) {
        genreService.deleteGenre(id);
        return ApiResponse.success(null);
    }
}
