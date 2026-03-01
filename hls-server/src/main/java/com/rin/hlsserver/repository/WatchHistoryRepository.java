package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.User;
import com.rin.hlsserver.model.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {
    
    Optional<WatchHistory> findByUserAndMovie(User user, Movie movie);
    
    @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.id = :userId ORDER BY wh.updatedAt DESC")
    Page<WatchHistory> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);
    
    long countByUserId(Long userId);
    
    void deleteByUserAndMovie(User user, Movie movie);
}
