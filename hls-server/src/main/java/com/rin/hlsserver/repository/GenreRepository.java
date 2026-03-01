package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByGenreId(String genreId);
    boolean existsByGenreId(String genreId);
}
