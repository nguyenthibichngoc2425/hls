package com.rin.hlsserver.controller;

import com.rin.hlsserver.exception.AppException;
import com.rin.hlsserver.exception.BaseErrorCode;
import com.rin.hlsserver.model.Movie;
import com.rin.hlsserver.monitor.service.MonitorTrackerService;
import com.rin.hlsserver.monitor.util.JwtHelper;
import com.rin.hlsserver.repository.MovieRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

@RestController
@RequestMapping("/api/hls")
@RequiredArgsConstructor
@Slf4j
public class HlsStreamingController {

    private static final long CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    
    private final MovieRepository movieRepository;
    private final MonitorTrackerService monitorTrackerService;

    @Value("${app.hls.storage-path:/home/nrin31266/hls-data/videos/hls}")
    private String hlsStoragePath;

    /**
     * GET /api/hls/{movieId}/master.m3u8
     * Lấy master playlist
     */
    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<String> getMasterPlaylist(
            @PathVariable Long movieId,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) throws IOException {
        log.info("GET /api/hls/{}/master.m3u8", movieId);
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        if (movie.getMasterPlaylistPath() == null) {
            throw new AppException(BaseErrorCode.VIDEO_NOT_PROCESSED);
        }

        File file = new File(movie.getMasterPlaylistPath());
        if (!file.exists()) {
            throw new AppException(BaseErrorCode.FILE_NOT_FOUND);
        }
        
        // Track master playlist request
        String email = (userEmail != null && !userEmail.isEmpty()) ? userEmail : JwtHelper.extractEmailFromRequest(request);
        monitorTrackerService.trackMasterPlaylist(request, email, String.valueOf(movieId));

        // Read and modify master playlist to add userEmail to variant playlists
        String playlistContent = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        if (userEmail != null && !userEmail.isEmpty()) {
            // Add userEmail parameter to each variant playlist
            playlistContent = playlistContent.replaceAll("(\\d+p/playlist\\.m3u8)", "$1?userEmail=" + userEmail);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.setCacheControl(CacheControl.noCache());
        headers.set("Accept-Ranges", "bytes");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(playlistContent);
    }

    /**
     * GET /api/hls/{movieId}/{quality}/playlist.m3u8
     * Lấy quality playlist (360p hoặc 720p)
     */
    @GetMapping("/{movieId}/{quality}/playlist.m3u8")
    public ResponseEntity<String> getQualityPlaylist(
            @PathVariable Long movieId,
            @PathVariable String quality,
            @RequestParam(required = false) String userEmail,
            HttpServletRequest request) throws IOException {
        
        log.info("GET /api/hls/{}/{}/playlist.m3u8", movieId, quality);
        
        // Validate quality
        if (!quality.equals("360p") && !quality.equals("720p")) {
            throw new AppException(BaseErrorCode.INVALID_QUALITY);
        }
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));

        File file = new File(hlsStoragePath + "/" + movieId + "/" + quality + "/playlist.m3u8");
        
        if (!file.exists()) {
            throw new AppException(BaseErrorCode.QUALITY_NOT_FOUND);
        }
        
        // Track playlist request and update online session
        String email = (userEmail != null && !userEmail.isEmpty()) ? userEmail : JwtHelper.extractEmailFromRequest(request);
        monitorTrackerService.trackPlaylist(request, email, String.valueOf(movieId), quality);

        // Read and modify playlist content to add userEmail to segment URLs
        String playlistContent = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        if (userEmail != null && !userEmail.isEmpty()) {
            // Add userEmail parameter to each .ts segment
            playlistContent = playlistContent.replaceAll("(segment_\\d+\\.ts)", "$1?userEmail=" + userEmail);
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
        headers.setCacheControl(CacheControl.noCache());
        headers.set("Accept-Ranges", "bytes");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(playlistContent);
    }

    /**
     * GET /api/hls/{movieId}/{quality}/{segmentName}
     * Lấy video segment (.ts file) với Range request support
     */
    @GetMapping("/{movieId}/{quality}/{segmentName:.+\\.ts}")
    public ResponseEntity<?> getSegment(
            @PathVariable Long movieId,
            @PathVariable String quality,
            @PathVariable String segmentName,
            @RequestParam(required = false) String userEmail,
            @RequestHeader(value = "Range", required = false) String range,
            HttpServletRequest request) throws IOException {
        
        log.info("GET /api/hls/{}/{}/{}", movieId, quality, segmentName);
        
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

        File file = new File(hlsStoragePath + "/" + movieId + "/" + quality + "/" + segmentName);
        
        if (!file.exists()) {
            throw new AppException(BaseErrorCode.SEGMENT_NOT_FOUND);
        }
        
        // Track segment request and update online session
        String email = (userEmail != null && !userEmail.isEmpty()) ? userEmail : JwtHelper.extractEmailFromRequest(request);
        monitorTrackerService.trackSegment(request, email, String.valueOf(movieId), quality, segmentName);

        FileSystemResource resource = new FileSystemResource(file);
        long fileLength = resource.contentLength();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp2t"));
        headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic());
        headers.set("Accept-Ranges", "bytes");

        // Handle range requests for better streaming
        if (range == null || range.isEmpty()) {
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileLength)
                    .body(resource);
        }

        // Parse range header
        long start = 0;
        try {
            String[] parts = range.replace("bytes=", "").split("-");
            start = Long.parseLong(parts[0]);
        } catch (Exception e) {
            log.warn("Invalid range header: {}", range);
        }

        long end = Math.min(start + CHUNK_SIZE - 1, fileLength - 1);
        HttpRange httpRange = HttpRange.createByteRange(start, end);
        ResourceRegion region = new ResourceRegion(resource, start, end - start + 1);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                .body(region);
    }

    /**
     * GET /api/hls/{movieId}/ready
     * Kiểm tra movie có sẵn sàng để stream không
     */
    @GetMapping("/{movieId}/ready")
    public ResponseEntity<Boolean> checkMovieReady(@PathVariable Long movieId) {
        log.info("GET /api/hls/{}/ready", movieId);
        
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new AppException(BaseErrorCode.MOVIE_NOT_FOUND));
        
        boolean ready = movie.getStatus() == Movie.MovieStatus.PUBLISHED && 
                       movie.getMasterPlaylistPath() != null;
        
        return ResponseEntity.ok(ready);
    }
}
