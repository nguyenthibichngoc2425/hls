package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.FavoriteMovie;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteMovieRepository extends JpaRepository<FavoriteMovie, Long> {
    
    boolean existsByUserAndMovie(User user, Movie movie);
    
    Optional<FavoriteMovie> findByUserAndMovie(User user, Movie movie);
    
    @Query("SELECT fm FROM FavoriteMovie fm WHERE fm.user.id = :userId ORDER BY fm.createdAt DESC")
    Page<FavoriteMovie> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    long countByUserId(Long userId);
    
    void deleteByUserAndMovie(User user, Movie movie);
    
    @Query("SELECT fm.movie.id FROM FavoriteMovie fm WHERE fm.user.id = :userId")
    List<Long> findMovieIdsByUserId(Long userId);
}
