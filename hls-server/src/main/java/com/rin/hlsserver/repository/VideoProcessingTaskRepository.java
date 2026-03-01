package com.rin.hlsserver.repository;

import com.rin.hlsserver.model.VideoProcessingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoProcessingTaskRepository extends JpaRepository<VideoProcessingTask, Long> {
    
    List<VideoProcessingTask> findByStatus(VideoProcessingTask.TaskStatus status);
    
    Optional<VideoProcessingTask> findByMovie_IdAndStatus(Long movieId, VideoProcessingTask.TaskStatus status);
    
    List<VideoProcessingTask> findByMovie_Id(Long movieId);
    
    void deleteByMovie_Id(Long movieId);
}
