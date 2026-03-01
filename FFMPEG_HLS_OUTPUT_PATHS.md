# FFmpeg HLS Output Path - Technical Reference

## Overview

This document explains how FFmpeg HLS output paths work and why the fix was necessary.

---

## FFmpeg HLS Parameters

### Output Playlist Path

**Parameter**: Final argument to FFmpeg command

```bash
ffmpeg -i input.mp4 [other options] -f hls OUTPUT_PLAYLIST_PATH
```

**Behavior**: 
- FFmpeg creates or overwrites the playlist file at this path
- By default, segments are created in the same directory as the playlist

**Example**:
```bash
ffmpeg -i video.mp4 -f hls C:\output\playlist.m3u8
```
→ Creates `C:\output\playlist.m3u8`
→ Creates segments in `C:\output\` directory

---

### Segment Filename Pattern

**Parameter**: `-hls_segment_filename PATTERN`

**Behavior**:
- Specifies where and with what pattern to save segment files
- Pattern variables: `%d` or `%03d` (3-digit zero-padded number)
- Recommended: Include full path, not just filename

**Examples**:

Good practice:
```bash
-hls_segment_filename C:\output\segment_%03d.ts
```
→ Creates segments at exact location: `C:\output\segment_000.ts`, `segment_001.ts`, ...

Poor practice:
```bash
-hls_segment_filename segment_%03d.ts
```
→ Creates segments relative to current working directory (unpredictable)

---

## The Bug

### What Was Happening

**buildFFmpegCommand() was using wrong directory:**

```java
String segmentPattern = Paths.get(movieDir, "segment_%03d.ts").toString();
// movieDir = "C:\hls\data\2"
// segmentPattern = "C:\hls\data\2\segment_%03d.ts"  ❌ WRONG

String playlistPath = Paths.get(movieDir, "playlist.m3u8").toString();
// playlistPath = "C:\hls\data\2\playlist.m3u8"  ❌ WRONG
```

Both 360p and 720p encoding used:
- **Segment path**: `C:\hls\data\2\segment_%03d.ts`
- **Playlist path**: `C:\hls\data\2\playlist.m3u8`

**Result**: 720p encoding overwrote 360p files!

---

## The Fix

### Using Correct Directory

**buildFFmpegCommand() now uses qualityDir:**

```java
String segmentPattern = Paths.get(outputDir, "segment_%03d.ts").toString();
// outputDir = "C:\hls\data\2\360p" (for 360p)
// outputDir = "C:\hls\data\2\720p" (for 720p)
// segmentPattern = "C:\hls\data\2\360p\segment_%03d.ts"  ✅ CORRECT

String qualityPlaylistPath = Paths.get(outputDir, "playlist.m3u8").toString();
// qualityPlaylistPath = "C:\hls\data\2\360p\playlist.m3u8"  ✅ CORRECT
```

Now 360p and 720p use different directories:
- **360p segment path**: `C:\hls\data\2\360p\segment_%03d.ts`
- **360p playlist path**: `C:\hls\data\2\360p\playlist.m3u8`
- **720p segment path**: `C:\hls\data\2\720p\segment_%03d.ts`
- **720p playlist path**: `C:\hls\data\2\720p\playlist.m3u8`

**Result**: No overwrites, both qualities preserved!

---

## FFmpeg Command Generation

### Method Signature

```java
private List<String> buildFFmpegCommand(
    String inputPath,           // Input video file path
    String outputDir,           // Output directory (qualityDir: 360p or 720p folder)
    VideoQuality.Quality quality,
    int durationSeconds,
    String movieDir,            // Movie root directory (NOT used for segments)
    boolean useGpu
)
```

### Parameter `outputDir`

**Passed from `processQuality()`:**

```java
private VideoQuality processQuality(...) {
    // Create quality-specific directory
    Path qualityDirPath = Paths.get(movieDir, quality.getLabel());
    // qualityDirPath = "C:\hls\data\2\360p" or "C:\hls\data\2\720p"
    
    String qualityDir = qualityDirPath.toString();
    Files.createDirectories(qualityDirPath);  // Ensure directory exists
    
    // Pass qualityDir to buildFFmpegCommand
    List<String> command = buildFFmpegCommand(
        sourceVideoPath,
        qualityDir,  // ← This is the outputDir parameter
        quality,
        durationSeconds,
        movieDir,
        true
    );
}
```

### How outputDir is Used

```java
// In buildFFmpegCommand:
String segmentPattern = Paths.get(outputDir, "segment_%03d.ts").toString();
// If outputDir = "C:\hls\data\2\360p"
// Then segmentPattern = "C:\hls\data\2\360p\segment_%03d.ts"

// Adds to FFmpeg command:
command.add("-hls_segment_filename");
command.add(segmentPattern);  // FFmpeg will save segments here

// And for the playlist:
String qualityPlaylistPath = Paths.get(outputDir, "playlist.m3u8").toString();
// If outputDir = "C:\hls\data\2\360p"
// Then qualityPlaylistPath = "C:\hls\data\2\360p\playlist.m3u8"

// Adds to FFmpeg command:
command.add(qualityPlaylistPath);  // FFmpeg will save playlist here
```

---

## Resulting FFmpeg Commands

### For 360p Quality

```bash
ffmpeg \
  -y \
  -i "C:\videos\input.mp4" \
  -t 1297 \
  -c:v h264_nvenc \
  -preset fast \
  -vf "scale=-2:360" \
  -b:v 800k \
  -maxrate 1300k \
  -bufsize 1600k \
  -c:a aac \
  -b:a 128k \
  -ar 44100 \
  -f hls \
  -hls_time 6 \
  -hls_list_size 0 \
  -hls_segment_filename "C:\hls\data\2\360p\segment_%03d.ts" \
  "C:\hls\data\2\360p\playlist.m3u8"
```

**FFmpeg will create:**
- Directory: `C:\hls\data\2\360p\` (must exist)
- Segments: `C:\hls\data\2\360p\segment_000.ts`, `segment_001.ts`, ...
- Playlist: `C:\hls\data\2\360p\playlist.m3u8`

---

### For 720p Quality

```bash
ffmpeg \
  -y \
  -i "C:\videos\input.mp4" \
  -t 1297 \
  -c:v h264_nvenc \
  -preset fast \
  -vf "scale=-2:720" \
  -b:v 2500k \
  -maxrate 3000k \
  -bufsize 5000k \
  -c:a aac \
  -b:a 128k \
  -ar 44100 \
  -f hls \
  -hls_time 6 \
  -hls_list_size 0 \
  -hls_segment_filename "C:\hls\data\2\720p\segment_%03d.ts" \
  "C:\hls\data\2\720p\playlist.m3u8"
```

**FFmpeg will create:**
- Directory: `C:\hls\data\2\720p\` (must exist)
- Segments: `C:\hls\data\2\720p\segment_000.ts`, `segment_001.ts`, ...
- Playlist: `C:\hls\data\2\720p\playlist.m3u8`

---

## Key Parameters

| Parameter | 360p Value | 720p Value |
|-----------|-----------|-----------|
| `-vf scale` | `-2:360` | `-2:720` |
| `-b:v` (bitrate) | `800k` | `2500k` |
| `-hls_segment_filename` | `C:\hls\data\2\360p\...` | `C:\hls\data\2\720p\...` |
| Output playlist | `C:\hls\data\2\360p\playlist.m3u8` | `C:\hls\data\2\720p\playlist.m3u8` |

---

## Master Playlist Creation

**After both qualities are processed, master.m3u8 is created:**

```java
String masterPath = Paths.get(movieDir, "master.m3u8").toString();
// masterPath = "C:\hls\data\2\master.m3u8"

try (BufferedWriter writer = new BufferedWriter(new FileWriter(masterPath))) {
    writer.write("#EXTM3U\n");
    writer.write("#EXT-X-VERSION:3\n\n");

    for (VideoQuality q : qualities) {
        // For 360p: writes "360p/playlist.m3u8"
        // For 720p: writes "720p/playlist.m3u8"
        writer.write(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%s\n",
                q.getBitrate() * 1000, q.getResolution()));
        writer.write(q.getQuality().getLabel() + "/playlist.m3u8\n\n");
    }
}
```

**Result `master.m3u8`:**
```m3u8
#EXTM3U
#EXT-X-VERSION:3

#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720
360p/playlist.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1920x1080
720p/playlist.m3u8
```

---

## Playback Flow

1. **Client requests**: `GET /stream/master.m3u8`
2. **Server returns**: Content of `C:\hls\data\2\master.m3u8`
3. **Client parses** master playlist, sees two variants (360p, 720p)
4. **Client selects**: 720p variant
5. **Client requests**: `GET /stream/720p/playlist.m3u8`
6. **Server returns**: Content of `C:\hls\data\2\720p\playlist.m3u8`
7. **Client parses** quality playlist, sees segments
8. **Client fetches segments**:
   - `GET /stream/720p/segment_000.ts`
   - `GET /stream/720p/segment_001.ts`
   - etc.
9. **Client plays** video stream

---

## Directory Structure Verification

After video processing, verify the structure:

```
C:\hls\data\2\
├── master.m3u8
│   (references: 360p/playlist.m3u8, 720p/playlist.m3u8)
│
├── 360p\
│   ├── playlist.m3u8
│   │   (references: segment_000.ts, segment_001.ts, ...)
│   ├── segment_000.ts
│   ├── segment_001.ts
│   ├── segment_002.ts
│   └── segment_003.ts
│
└── 720p\
    ├── playlist.m3u8
    │   (references: segment_000.ts, segment_001.ts, ...)
    ├── segment_000.ts
    ├── segment_001.ts
    ├── segment_002.ts
    └── segment_003.ts
```

---

## References

### FFmpeg HLS Documentation
- `-f hls` - HLS format
- `-hls_time` - Segment duration (seconds)
- `-hls_list_size` - Playlist size (0 = keep all)
- `-hls_segment_filename` - Output segment pattern

### HLS Specification
- RFC 8216 - HTTP Live Streaming (HLS)
- MPEG-2 TS segments
- UTF-8 encoding for playlists

---

**Status**: ✅ Fix verified and documented
