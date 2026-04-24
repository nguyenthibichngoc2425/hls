package raven.modal.demo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import raven.modal.demo.dto.request.MovieReviewRequest;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.MovieRatingSummaryResponse;
import raven.modal.demo.dto.response.MovieReviewResponse;

import java.util.List;

public class ReviewApi {

    public static ApiResponse<List<MovieReviewResponse>> getMovieReviews(Long movieId, int page, int size) {
        try {
            String path = String.format("/movies/%d/reviews?page=%d&size=%d", movieId, page, size);
            TypeReference<ApiResponse<List<MovieReviewResponse>>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<MovieReviewResponse> upsertReview(Long movieId, MovieReviewRequest request) {
        try {
            String path = String.format("/movies/%d/reviews", movieId);
            TypeReference<ApiResponse<MovieReviewResponse>> type = new TypeReference<>() {};
            return Http.post(path, request, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<MovieRatingSummaryResponse> getMovieRatingSummary(Long movieId) {
        try {
            String path = String.format("/movies/%d/reviews/summary", movieId);
            TypeReference<ApiResponse<MovieRatingSummaryResponse>> type = new TypeReference<>() {};
            return Http.get(path, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }
}
