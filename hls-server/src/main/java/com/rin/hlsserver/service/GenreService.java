package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.request.GenreRequest;
import com.rin.hlsserver.dto.response.GenreResponse;
import com.rin.hlsserver.exception.BaseException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Genre;
import com.rin.hlsserver.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreRepository genreRepository;

    @Transactional(readOnly = true)
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GenreResponse getGenreById(Long id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new BaseException(BaseErrorCode.GENRE_NOT_FOUND));
        return toResponse(genre);
    }

    @Transactional
    public GenreResponse createGenre(GenreRequest request) {
        // Kiểm tra genreId đã tồn tại chưa
        if (genreRepository.existsByGenreId(request.getGenreId())) {
            throw new BaseException(BaseErrorCode.GENRE_ALREADY_EXISTS);
        }

        Genre genre = Genre.builder()
                .genreId(request.getGenreId().toLowerCase())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        genre = genreRepository.save(genre);
        return toResponse(genre);
    }

    @Transactional
    public GenreResponse updateGenre(Long id, GenreRequest request) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new BaseException(BaseErrorCode.GENRE_NOT_FOUND));

        // Nếu thay đổi genreId, kiểm tra trùng
        if (!genre.getGenreId().equals(request.getGenreId().toLowerCase())) {
            if (genreRepository.existsByGenreId(request.getGenreId())) {
                throw new BaseException(BaseErrorCode.GENRE_ALREADY_EXISTS);
            }
            genre.setGenreId(request.getGenreId().toLowerCase());
        }

        genre.setName(request.getName());
        genre.setDescription(request.getDescription());

        genre = genreRepository.save(genre);
        return toResponse(genre);
    }

    @Transactional
    public void deleteGenre(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new BaseException(BaseErrorCode.GENRE_NOT_FOUND);
        }
        genreRepository.deleteById(id);
    }

    private GenreResponse toResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .genreId(genre.getGenreId())
                .name(genre.getName())
                .description(genre.getDescription())
                .createdAt(genre.getCreatedAt())
                .updatedAt(genre.getUpdatedAt())
                .build();
    }
}
