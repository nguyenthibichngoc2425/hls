package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.VideoQuality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoQualityRepository extends JpaRepository<VideoQuality, Long> {
    
    List<VideoQuality> findByMovie_Id(Long movieId);
    
    Optional<VideoQuality> findByMovie_IdAndQuality(Long movieId, VideoQuality.Quality quality);
    
    void deleteByMovie_Id(Long movieId);
}
