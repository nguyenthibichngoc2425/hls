# HLS Folder Structure Fix - Summary

## Issue
Segments and playlists were being generated in the movie root folder instead of quality-specific subfolders, causing 360p and 720p files to overwrite each other.

## Root Cause
In `buildFFmpegCommand()`, the segment and playlist output paths were using `movieDir` instead of `outputDir` (which is the quality-specific folder).

## Fix Applied
**File**: `FFmpegService.java` - Lines 235-240

### Before ❌
```java
String segmentPattern = Paths.get(movieDir, "segment_%03d.ts").toString();
String playlistPath = Paths.get(movieDir, "playlist.m3u8").toString();
```

### After ✅
```java
String segmentPattern = Paths.get(outputDir, "segment_%03d.ts").toString();
String qualityPlaylistPath = Paths.get(outputDir, "playlist.m3u8").toString();
```

## Result

### Before ❌
```
C:\hls\data\2\
├── master.m3u8
├── segment_000.ts ← 720p only (360p overwritten)
├── segment_001.ts
└── playlist.m3u8 ← 720p only
```

### After ✅
```
C:\hls\data\2\
├── master.m3u8
├── 360p\
│   ├── segment_000.ts
│   ├── segment_001.ts
│   └── playlist.m3u8
└── 720p\
    ├── segment_000.ts
    ├── segment_001.ts
    └── playlist.m3u8
```

## Verification

Run this after video processing:
```powershell
tree C:\hls\data\<movieId>
```

Expected output shows:
- ✅ `360p/` folder with segments and playlist
- ✅ `720p/` folder with segments and playlist
- ✅ `master.m3u8` at root

## Documentation

See detailed explanations:
- [HLS_FOLDER_STRUCTURE_FIX.md](HLS_FOLDER_STRUCTURE_FIX.md) - Technical details
- [HLS_STRUCTURE_BEFORE_AFTER.md](HLS_STRUCTURE_BEFORE_AFTER.md) - Visual comparison
- [FFMPEG_HLS_OUTPUT_PATHS.md](FFMPEG_HLS_OUTPUT_PATHS.md) - FFmpeg parameters reference

## Status
✅ Fixed - Ready for testing
