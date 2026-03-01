package raven.modal.demo.api;

import com.fasterxml.jackson.core.type.TypeReference;
import raven.modal.demo.dto.request.GenreRequest;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.GenreResponse;

import java.util.List;

public class GenreApi {

    public static ApiResponse<List<GenreResponse>> getAllGenres() {
        try {
            TypeReference<ApiResponse<List<GenreResponse>>> type = new TypeReference<>() {};
            return Http.get("/genres", type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<GenreResponse> getGenreById(Long id) {
        try {
            TypeReference<ApiResponse<GenreResponse>> type = new TypeReference<>() {};
            return Http.get("/genres/" + id, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<GenreResponse> createGenre(GenreRequest request) {
        try {
            TypeReference<ApiResponse<GenreResponse>> type = new TypeReference<>() {};
            return Http.post("/genres", request, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<GenreResponse> updateGenre(Long id, GenreRequest request) {
        try {
            TypeReference<ApiResponse<GenreResponse>> type = new TypeReference<>() {};
            return Http.put("/genres/" + id, request, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }

    public static ApiResponse<Void> deleteGenre(Long id) {
        try {
            TypeReference<ApiResponse<Void>> type = new TypeReference<>() {};
            return Http.delete("/genres/" + id, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>("Lỗi kết nối: " + e.getMessage(), null, 500);
        }
    }
}
