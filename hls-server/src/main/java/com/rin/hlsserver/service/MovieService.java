package com.rin.hlsserver.service;

import com.rin.hlsserver.dto.request.MovieRequest;
import com.rin.hlsserver.dto.response.ApiResponse;
import com.rin.hlsserver.dto.response.MovieResponse;
import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Genre;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.model.VideoProcessingTask;
import com.rin.hlsserver.model.VideoQuality;
import com.rin.hlsserver.repository.GenreRepository;
import com.rin.hlsserver.repository.MovieRepository;
import com.rin.hlsserver.repository.VideoProcessingTaskRepository;
import com.rin.hlsserver.repository.VideoQualityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {
    
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final VideoQualityRepository videoQualityRepository;
    private final VideoProcessingTaskRepository taskRepository;
    private final FFmpegService ffmpegService;
    private final ApplicationContext applicationContext;
    
    /**
     * Lấy tất cả movies
     */
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Tìm kiếm movies theo tên (chỉ lấy status PUBLISHED)
     */
    public List<MovieResponse> searchMovies(String keyword) {
        return movieRepository.findByTitleContainingIgnoreCaseAndStatus(keyword, Movie.MovieStatus.PUBLISHED).stream()
                .map(MovieResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy movie theo ID
     */
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findByIdWithQualities(id)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        return MovieResponse.fromEntity(movie);
    }
    
    /**
     * Tạo movie mới
     */
    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        // Validate genre exists
        Genre genre = genreRepository.findById(request.getGenreId())
                .orElseThrow(() -> new AppException(BaseErrorCode.GENRE_NOT_FOUND));
        
        // Validate processing minutes
        if (request.getProcessingMinutes() < 0) {
            throw new AppException(BaseErrorCode.INVALID_REQUEST, 
                    "Processing minutes must be >= 0");
        }
        
        // Tạo movie entity
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .sourceVideoPath(request.getSourceVideoPath())
                .duration(request.getDuration())
                .processingMinutes(request.getProcessingMinutes())
                .releaseYear(request.getReleaseYear())
                .genre(genre)
                .status(Movie.MovieStatus.DRAFT)
                .processingProgress(0)
                .build();
        
        movie = movieRepository.save(movie);
        log.info("Created movie with ID: {}", movie.getId());
        
        // Nếu processingMinutes > 0, bắt đầu xử lý video async
        if (request.getProcessingMinutes() > 0) {
            movie.setStatus(Movie.MovieStatus.PROCESSING);
            movie = movieRepository.save(movie);
            
            // Tạo processing task
            VideoProcessingTask task = VideoProcessingTask.builder()
                    .movie(movie)
                    .status(VideoProcessingTask.TaskStatus.PENDING)
                    .progress(0)
                    .processingDurationMinutes(request.getProcessingMinutes())
                    .build();
            task = taskRepository.saveAndFlush(task); // Flush ngay để async query được
            
            // Xử lý async - đợi transaction commit xong mới chạy
            log.info("Controller returning response immediately, video processing in background...");
            Long finalTaskId = task.getId();
            Long finalMovieId = movie.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    applicationContext.getBean(MovieService.class).processVideoAsync(finalMovieId, finalTaskId);
                }
            });
        }

        if(request.getProcessingMinutes() == 0){
            movie.setStatus(Movie.MovieStatus.PUBLISHED);
        }
        
        return MovieResponse.fromEntity(movie);
    }
    
    /**
     * Update movie
     */
    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        
        // Validate genre
        Genre genre = genreRepository.findById(request.getGenreId())
                .orElseThrow(() -> new AppException(BaseErrorCode.GENRE_NOT_FOUND));
        
        // Update fields
        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setImageUrl(request.getImageUrl());
        movie.setSourceVideoPath(request.getSourceVideoPath());
        movie.setDuration(request.getDuration());
        movie.setReleaseYear(request.getReleaseYear());
        movie.setGenre(genre);
        
        // Nếu processingMinutes thay đổi và movie đang không xử lý
        if (!movie.getProcessingMinutes().equals(request.getProcessingMinutes()) &&
                movie.getStatus() != Movie.MovieStatus.PROCESSING) {
            
            movie.setProcessingMinutes(request.getProcessingMinutes());
            
            // Nếu chuyển từ 0 sang > 0, bắt đầu xử lý
            if (request.getProcessingMinutes() > 0) {
                // Xóa video cũ nếu có
                ffmpegService.deleteMovieVideos(movie.getId());
                videoQualityRepository.deleteByMovie_Id(movie.getId());
                
                movie.setStatus(Movie.MovieStatus.PROCESSING);
                movie.setProcessingProgress(0);
                movie.setProcessingError(null);
                movie.setMasterPlaylistPath(null);
                
                // Tạo task mới
                VideoProcessingTask task = VideoProcessingTask.builder()
                        .movie(movie)
                        .status(VideoProcessingTask.TaskStatus.PENDING)
                        .progress(0)
                        .processingDurationMinutes(request.getProcessingMinutes())
                        .build();
                task = taskRepository.saveAndFlush(task); // Flush ngay
                
                movie = movieRepository.save(movie);
                log.info("Update returning immediately, video processing in background...");
                
                Long finalTaskId = task.getId();
                Long finalMovieId = movie.getId();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        applicationContext.getBean(MovieService.class).processVideoAsync(finalMovieId, finalTaskId);
                    }
                });
            }
        }
        if(request.getProcessingMinutes() == 0 && movie.getStatus() != Movie.MovieStatus.PROCESSING){
            movie.setStatus(Movie.MovieStatus.PUBLISHED);
        }
        
        movie = movieRepository.save(movie);
        return MovieResponse.fromEntity(movie);
    }
    
    /**
     * Xóa movie
     */
    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        
        // Xóa video files
        ffmpegService.deleteMovieVideos(id);
        
        // Xóa database records theo thứ tự (foreign key constraints)
        taskRepository.deleteByMovie_Id(id); // Xóa tasks trước
        videoQualityRepository.deleteByMovie_Id(id);
        movieRepository.delete(movie);
        
        log.info("Deleted movie with ID: {}", id);
    }
    
    /**
     * Re-process video
     */
    @Transactional
    public MovieResponse reprocessMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        
        if (movie.getProcessingMinutes() == 0) {
            throw new AppException(BaseErrorCode.INVALID_REQUEST, 
                    "Cannot reprocess movie with processingMinutes = 0");
        }
        
        if (movie.getStatus() == Movie.MovieStatus.PROCESSING) {
            throw new AppException(BaseErrorCode.INVALID_REQUEST, 
                    "Movie is already being processed");
        }
        
        // Xóa video cũ
        ffmpegService.deleteMovieVideos(movie.getId());
        videoQualityRepository.deleteByMovie_Id(movie.getId());
        
        // Reset status
        movie.setStatus(Movie.MovieStatus.PROCESSING);
        movie.setProcessingProgress(0);
        movie.setProcessingError(null);
        movie.setMasterPlaylistPath(null);
        movie = movieRepository.save(movie);
        
        // Tạo task mới
        VideoProcessingTask task = VideoProcessingTask.builder()
                .movie(movie)
                .status(VideoProcessingTask.TaskStatus.PENDING)
                .progress(0)
                .processingDurationMinutes(movie.getProcessingMinutes())
                .build();
        task = taskRepository.saveAndFlush(task); // Flush ngay
        
        log.info("Reprocess returning immediately, video processing in background...");
        
        Long finalTaskId = task.getId();
        Long finalMovieId = movie.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationContext.getBean(MovieService.class).processVideoAsync(finalMovieId, finalTaskId);
            }
        });
        
        return MovieResponse.fromEntity(movie);
    }
    
    /**
     * Xử lý video async - KHÔNG dùng @Transactional để tránh block caller
     */
    @Async
    public void processVideoAsync(Long movieId, Long taskId) {
        log.info("Starting async video processing for movie ID: {}, task ID: {}", movieId, taskId);
        
        VideoProcessingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found"));
        
        try {
            // Update task status
            task.setStatus(VideoProcessingTask.TaskStatus.RUNNING);
            task.setStartedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            // Throttle progress updates - chỉ update khi thay đổi >= 5%
            final int[] lastProgress = {0};
            
            // Process video with FFmpeg - truyền callback để update progress
            List<VideoQuality> qualities = ffmpegService.processVideoToHLS(
                    movie, 
                    movie.getSourceVideoPath(), 
                    movie.getProcessingMinutes(),
                    progress -> {
                        // Chỉ update khi progress thay đổi đủ lớn (>= 5%) để giảm DB queries
                        if (progress - lastProgress[0] >= 5 || progress >= 100) {
                            lastProgress[0] = progress;
                            try {
                                task.setProgress(progress);
                                movie.setProcessingProgress(progress);
                                taskRepository.save(task);
                                movieRepository.save(movie);
                                log.info("Updated progress for movie {}: {}%", movieId, progress);
                            } catch (Exception e) {
                                log.error("Error updating progress", e);
                            }
                        }
                    }
            );
            
            // Save video qualities
            for (VideoQuality quality : qualities) {
                videoQualityRepository.save(quality);
            }
            
            // Update movie status
            movie.setStatus(Movie.MovieStatus.PUBLISHED);
            movie.setProcessingProgress(100);
            movie.setProcessingError(null);
            movieRepository.save(movie);
            
            // Update task
            task.setStatus(VideoProcessingTask.TaskStatus.COMPLETED);
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
            
            log.info("Successfully completed video processing for movie ID: {}", movieId);
            
        } catch (Exception e) {
            log.error("Error processing video for movie ID: {}", movieId, e);
            
            // Update movie status
            movie.setStatus(Movie.MovieStatus.FAILED);
            movie.setProcessingError(e.getMessage());
            movieRepository.save(movie);
            
            // Update task
            task.setStatus(VideoProcessingTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            taskRepository.save(task);
        }
    }
}
