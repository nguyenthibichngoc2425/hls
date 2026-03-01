package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.request.MovieRequest;
import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.MovieResponse;
import com.rin.hlsserver.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieController {
    
    private final MovieService movieService;
    
    @GetMapping
    public ApiResponse<List<MovieResponse>> getAllMovies() {
        log.info("GET /api/movies - Get all movies");
        List<MovieResponse> movies = movieService.getAllMovies();
        return ApiResponse.<List<MovieResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get all movies successfully")
                .result(movies)
                .build();
    }
    
    @GetMapping("/search")
    public ApiResponse<List<MovieResponse>> searchMovies(@RequestParam String keyword) {
        log.info("GET /api/movies/search?keyword={} - Search movies", keyword);
        List<MovieResponse> movies = movieService.searchMovies(keyword);
        return ApiResponse.<List<MovieResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Search movies successfully")
                .result(movies)
                .build();
    }
    
    @GetMapping("/{id}")
    public ApiResponse<MovieResponse> getMovieById(@PathVariable Long id) {
        log.info("GET /api/movies/{} - Get movie by ID", id);
        MovieResponse movie = movieService.getMovieById(id);
        return ApiResponse.<MovieResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Get movie successfully")
                .result(movie)
                .build();
    }
    
    @PostMapping
    public ApiResponse<MovieResponse> createMovie(@Valid @RequestBody MovieRequest request) {
        log.info("POST /api/movies - Create new movie: {}", request.getTitle());
        MovieResponse movie = movieService.createMovie(request);
        return ApiResponse.<MovieResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Movie created successfully")
                .result(movie)
                .build();
    }
    
    @PutMapping("/{id}")
    public ApiResponse<MovieResponse> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request) {
        log.info("PUT /api/movies/{} - Update movie", id);
        MovieResponse movie = movieService.updateMovie(id, request);
        return ApiResponse.<MovieResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Movie updated successfully")
                .result(movie)
                .build();
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMovie(@PathVariable Long id) {
        log.info("DELETE /api/movies/{} - Delete movie", id);
        movieService.deleteMovie(id);
        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Movie deleted successfully")
                .build();
    }
    
    @PostMapping("/{id}/reprocess")
    public ApiResponse<MovieResponse> reprocessMovie(@PathVariable Long id) {
        log.info("POST /api/movies/{}/reprocess - Reprocess movie", id);
        MovieResponse movie = movieService.reprocessMovie(id);
        return ApiResponse.<MovieResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Movie reprocessing started")
                .result(movie)
                .build();
    }
}
