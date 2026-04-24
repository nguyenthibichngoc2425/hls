package com.rin.hlsserver.controller;

import com.rin.hlsserver.dto.request.MovieReviewRequest;
import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.MovieRatingSummaryResponse;
import com.rin.hlsserver.dto.response.MovieReviewResponse;
import com.rin.hlsserver.service.MovieReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies/{movieId}/reviews")
@RequiredArgsConstructor
@Slf4j
public class MovieReviewController {

    private final MovieReviewService movieReviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<MovieReviewResponse>> upsertReview(
            @PathVariable Long movieId,
            @Valid @RequestBody MovieReviewRequest request) {

        MovieReviewResponse response = movieReviewService.upsertReview(movieId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieReviewResponse>>> getMovieReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<MovieReviewResponse> response = movieReviewService.getMovieReviews(movieId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MovieRatingSummaryResponse>> getMovieRatingSummary(
            @PathVariable Long movieId) {

        MovieRatingSummaryResponse response = movieReviewService.getMovieRatingSummary(movieId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
