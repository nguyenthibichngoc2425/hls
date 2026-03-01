package com.rin.hlsserver.service;

import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
@Slf4j
public class HlsStreamingService {

    private final MovieRepository movieRepository;

    @Value("${app.hls.storage-path:/home/nrin31266/hls-data/videos/hls}")
    private String hlsStoragePath;

    /**
     * Lấy master playlist cho movie
     */
    public Resource getMasterPlaylist(Long movieId) {
        log.info("Getting master playlist for movie ID: {}", movieId);
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        if (movie.getMasterPlaylistPath() == null) {
            throw new AppException(BaseErrorCode.VIDEO_NOT_PROCESSED);
        }

        return loadResource(movie.getMasterPlaylistPath());
    }

    /**
     * Lấy quality playlist (360p/playlist.m3u8 hoặc 720p/playlist.m3u8)
     */
    public Resource getQualityPlaylist(Long movieId, String quality) {
        log.info("Getting {} playlist for movie ID: {}", quality, movieId);
        
        // Validate quality
        if (!quality.equals("360p") && !quality.equals("720p")) {
            throw new AppException(BaseErrorCode.INVALID_QUALITY);
        }
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        Path playlistPath = Paths.get(hlsStoragePath, movieId.toString(), quality, "playlist.m3u8");
        
        if (!Files.exists(playlistPath)) {
            throw new AppException(BaseErrorCode.QUALITY_NOT_FOUND);
        }

        return loadResource(playlistPath.toString());
    }

    /**
     * Lấy video segment (.ts file)
     */
    public Resource getSegment(Long movieId, String quality, String segmentName) {
        log.info("Getting segment {} for movie ID: {}, quality: {}", segmentName, movieId, quality);
        
        // Validate quality
        if (!quality.equals("360p") && !quality.equals("720p")) {
            throw new AppException(BaseErrorCode.INVALID_QUALITY);
        }
        
        // Validate segment name format (segment_XXX.ts)
        if (!segmentName.matches("segment_\\d{3}\\.ts")) {
            throw new AppException(BaseErrorCode.INVALID_SEGMENT);
        }
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        Path segmentPath = Paths.get(hlsStoragePath, movieId.toString(), quality, segmentName);
        
        if (!Files.exists(segmentPath)) {
            throw new AppException(BaseErrorCode.SEGMENT_NOT_FOUND);
        }

        return loadResource(segmentPath.toString());
    }

    /**
     * Load file resource từ path
     */
    private Resource loadResource(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AppException(BaseErrorCode.FILE_NOT_READABLE);
            }
        } catch (MalformedURLException e) {
            log.error("Error loading resource: {}", filePath, e);
            throw new AppException(BaseErrorCode.FILE_NOT_FOUND);
        }
    }

    /**
     * Kiểm tra movie có sẵn sàng để stream không
     */
    public boolean isMovieReady(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        
        return movie.getStatus() == Movie.MovieStatus.PUBLISHED && 
               movie.getMasterPlaylistPath() != null;
    }
}
