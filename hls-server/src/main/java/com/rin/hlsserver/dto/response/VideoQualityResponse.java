package com.rin.hlsserver.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rin.hlsserver.model.VideoQuality;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoQualityResponse {
    
    private Long id;
    private String quality;
    private String playlistPath;
    private String segmentsPath;
    private Integer bitrate;
    private String resolution;
    private Long fileSize;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
    
    public static VideoQualityResponse fromEntity(VideoQuality videoQuality) {
        return VideoQualityResponse.builder()
                .id(videoQuality.getId())
                .quality(videoQuality.getQuality().getLabel())
                .playlistPath(videoQuality.getPlaylistPath())
                .segmentsPath(videoQuality.getSegmentsPath())
                .bitrate(videoQuality.getBitrate())
                .resolution(videoQuality.getResolution())
                .fileSize(videoQuality.getFileSize())
                .createdAt(videoQuality.getCreatedAt())
                .build();
    }
}
