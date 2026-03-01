package raven.modal.demo.utils;

import raven.modal.demo.api.GenreApi;
import raven.modal.demo.dto.response.ApiResponse;
import raven.modal.demo.dto.response.GenreResponse;
import raven.modal.demo.model.Genre;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton quản lý danh sách thể loại phim
 * Load một lần khi khởi động và cache lại để dùng chung
 */
public class GenreManager {
    private static GenreManager instance;
    private List<GenreResponse> genreResponses;
    private List<Genre> genres;

    private GenreManager() {
        genreResponses = new ArrayList<>();
        genres = new ArrayList<>();
    }

    public static GenreManager getInstance() {
        if (instance == null) {
            instance = new GenreManager();
        }
        return instance;
    }

    /**
     * Load danh sách thể loại từ API
     */
    public void loadGenres() {
        try {
            ApiResponse<List<GenreResponse>> response = GenreApi.getAllGenres();
            if (response.getCode() == 200 && response.getResult() != null) {
                genreResponses = response.getResult();
                // Convert sang Genre model để dùng cho ComboBox
                genres = genreResponses.stream()
                        .map(gr -> new Genre(gr.getId(), gr.getName()))
                        .collect(Collectors.toList());
                System.out.println("Loaded " + genres.size() + " genres from server");
            }
        } catch (Exception e) {
            System.err.println("Failed to load genres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Thêm một genre mới vào cache (sau khi tạo thành công)
     */
    public void addGenre(GenreResponse genreResponse) {
        genreResponses.add(genreResponse);
        genres.add(new Genre(genreResponse.getId(), genreResponse.getName()));
    }

    /**
     * Xóa genre khỏi cache (sau khi xóa thành công)
     */
    public void removeGenre(Long id) {
        genreResponses.removeIf(gr -> gr.getId().equals(id));
        genres.removeIf(g -> g.getId().equals(id));
    }

    /**
     * Cập nhật genre trong cache (sau khi update thành công)
     */
    public void updateGenre(GenreResponse genreResponse) {
        for (int i = 0; i < genreResponses.size(); i++) {
            if (genreResponses.get(i).getId().equals(genreResponse.getId())) {
                genreResponses.set(i, genreResponse);
                genres.set(i, new Genre(genreResponse.getId(), genreResponse.getName()));
                break;
            }
        }
    }

    /**
     * Lấy danh sách Genre để dùng cho ComboBox
     */
    public List<Genre> getGenres() {
        return new ArrayList<>(genres);
    }

    /**
     * Lấy danh sách GenreResponse đầy đủ
     */
    public List<GenreResponse> getGenreResponses() {
        return new ArrayList<>(genreResponses);
    }

    /**
     * Lấy Genre[] để dùng cho ComboBox model
     */
    public Genre[] getGenreArray() {
        return genres.toArray(new Genre[0]);
    }
}
