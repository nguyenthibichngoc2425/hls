package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    Optional<Movie> findByTitle(String title);
    
    List<Movie> findByStatus(Movie.MovieStatus status);
    
    List<Movie> findByTitleContainingIgnoreCaseAndStatus(String title, Movie.MovieStatus status);
    
    List<Movie> findByGenre_GenreId(String genreId);
    
    @Query("SELECT m FROM Movie m WHERE m.status = 'PROCESSING'")
    List<Movie> findProcessingMovies();
    
    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.videoQualities WHERE m.id = :id")
    Optional<Movie> findByIdWithQualities(Long id);
}
