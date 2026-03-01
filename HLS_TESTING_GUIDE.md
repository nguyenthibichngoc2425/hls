# HLS Folder Structure - Testing Guide

## Pre-Test Setup

### Clean Previous Files
```powershell
# Remove old test data
Remove-Item -Recurse -Force "C:\hls\data\*"

# Create storage directory
mkdir "C:\hls\data"
```

### Start Application
```bash
mvn spring-boot:run -pl hls-server
```

---

## Test Case 1: Single Movie with Two Qualities

### Step 1: Create Movie via API
```bash
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test HLS Structure",
    "description": "Testing folder structure fix",
    "sourceVideoPath": "C:/hls/sources/test_video.mp4",
    "duration": 3600,
    "processingMinutes": 2,
    "releaseYear": 2024,
    "imageUrl": "http://example.com/image.jpg",
    "genreId": 1
  }'
```

**Response**:
```json
{
  "id": 1,
  "title": "Test HLS Structure",
  "processingProgress": 0,
  "status": "PROCESSING"
}
```

### Step 2: Monitor Progress
```bash
# Check status every 5 seconds
while($true) {
  Invoke-RestMethod -Uri "http://localhost:8080/api/movies/1" | Select-Object processingProgress, status
  Start-Sleep -Seconds 5
}
```

### Step 3: Verify Folder Structure (During Processing)

```powershell
# While processing, check folder structure
Get-ChildItem -Recurse "C:\hls\data\1" | Select-Object FullName | Format-List

# Expected output (while processing):
# FullName : C:\hls\data\1\360p
# FullName : C:\hls\data\1\360p\segment_000.ts
# FullName : C:\hls\data\1\360p\segment_001.ts
# FullName : C:\hls\data\1\720p
# FullName : C:\hls\data\1\720p\segment_000.ts
```

### Step 4: Verify After Completion

**After processing completes (status: PUBLISHED)**:

```powershell
# List all files recursively
tree "C:\hls\data\1" /F

# Expected output:
# C:\hls\data\1
# ├── master.m3u8
# ├── 360p
# │   ├── playlist.m3u8
# │   ├── segment_000.ts
# │   ├── segment_001.ts
# │   ├── segment_002.ts
# │   ├── segment_003.ts
# │   └── ...
# └── 720p
#     ├── playlist.m3u8
#     ├── segment_000.ts
#     ├── segment_001.ts
#     ├── segment_002.ts
#     ├── segment_003.ts
#     └── ...
```

---

## Test Case 2: Verify No Overwrites

### Step 1: Count Files

```powershell
# Count 360p segment files
(Get-ChildItem "C:\hls\data\1\360p\segment_*.ts").Count
# Expected: More than 0 (e.g., 20)

# Count 720p segment files
(Get-ChildItem "C:\hls\data\1\720p\segment_*.ts").Count
# Expected: More than 0 (e.g., 20)
```

### Verification
✅ **Both folders have segments** (no overwrites)
❌ If only one folder has segments → Fix not working

---

## Test Case 3: Verify File Sizes

```powershell
# 360p should be smaller than 720p
$size360 = (Get-ChildItem "C:\hls\data\1\360p\segment_*.ts" | Measure-Object -Property Length -Sum).Sum
$size720 = (Get-ChildItem "C:\hls\data\1\720p\segment_*.ts" | Measure-Object -Property Length -Sum).Sum

Write-Host "360p total size: $($size360/1MB)MB"
Write-Host "720p total size: $($size720/1MB)MB"

# Expected: 360p size < 720p size (both non-zero)
```

### Verification
✅ `360p < 720p` in size (different bit rates)
❌ `360p = 0` or `720p = 0` → Overwrites occurred

---

## Test Case 4: Verify Playlist Contents

### 360p Playlist
```powershell
Get-Content "C:\hls\data\1\360p\playlist.m3u8" | Select-Object -First 20

# Expected:
# #EXTM3U
# #EXT-X-VERSION:3
# #EXT-X-TARGETDURATION:7
# #EXTINF:6.0,
# segment_000.ts
# #EXTINF:6.0,
# segment_001.ts
# ...
```

### 720p Playlist
```powershell
Get-Content "C:\hls\data\1\720p\playlist.m3u8" | Select-Object -First 20

# Expected: Same structure as 360p
```

### Master Playlist
```powershell
Get-Content "C:\hls\data\1\master.m3u8"

# Expected:
# #EXTM3U
# #EXT-X-VERSION:3
# 
# #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=1280x720
# 360p/playlist.m3u8
# 
# #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1920x1080
# 720p/playlist.m3u8
```

### Verification
✅ All playlists present and properly formatted
✅ Master references correct quality playlists
❌ References point to wrong locations → Structure issue

---

## Test Case 5: Verify Video Playback

### Using FFmpeg
```bash
# Test 360p stream
ffplay "C:\hls\data\1\360p\playlist.m3u8"

# Test 720p stream
ffplay "C:\hls\data\1\720p\playlist.m3u8"

# Test master playlist
ffplay "C:\hls\data\1\master.m3u8"
```

### Using VLC
1. Open VLC media player
2. Media → Open Network Stream
3. Enter: `http://localhost:8080/stream/master.m3u8`
4. Should show both 360p and 720p options
5. Click each to verify playback

### Verification
✅ All streams play without errors
✅ Video quality varies between 360p and 720p
❌ Only one quality plays → Overwrite occurred
❌ Playback fails → File structure wrong

---

## Test Case 6: Database Verification

### Check Video Qualities Stored

```bash
# Check via API
curl http://localhost:8080/api/movies/1

# Expected response includes both qualities:
# "qualities": [
#   {
#     "quality": "360p",
#     "playlistPath": "C:\hls\data\1\360p\playlist.m3u8",
#     "resolution": "1280x720"
#   },
#   {
#     "quality": "720p",
#     "playlistPath": "C:\hls\data\1\720p\playlist.m3u8",
#     "resolution": "1920x1080"
#   }
# ]
```

### Verification
✅ Both 360p and 720p in response
❌ Only one quality → Overwrite occurred in database

---

## Test Case 7: Multiple Movies

### Create Two Movies
```bash
# Movie 1
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Movie1","processingMinutes":1,...}'

# Movie 2
curl -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Movie2","processingMinutes":1,...}'
```

### Verify Separate Folders
```powershell
# Should have independent folders
tree "C:\hls\data\1" /F  # Movie 1
tree "C:\hls\data\2" /F  # Movie 2

# Each should have complete structure:
# C:\hls\data\1\
#   ├── master.m3u8
#   ├── 360p\
#   │   └── playlist.m3u8 (multiple segments)
#   └── 720p\
#       └── playlist.m3u8 (multiple segments)
#
# C:\hls\data\2\
#   ├── master.m3u8
#   ├── 360p\
#   │   └── playlist.m3u8 (multiple segments)
#   └── 720p\
#       └── playlist.m3u8 (multiple segments)
```

### Verification
✅ Each movie has independent complete structure
❌ Overlapping files → ID folder issue

---

## Test Case 8: Re-processing Same Movie

### Reprocess Movie
```bash
curl -X POST http://localhost:8080/api/movies/1/reprocess \
  -H "Content-Type: application/json"
```

### Verify Old Files Deleted
```powershell
# Check segment count before reprocess
(Get-ChildItem "C:\hls\data\1\360p\segment_*.ts").Count
# Remember this number

# After reprocess completes, check again
(Get-ChildItem "C:\hls\data\1\360p\segment_*.ts").Count
# Should be similar (not accumulated)
```

### Verification
✅ Old segments removed before reprocessing
✅ New segments created
❌ Segment count doubled → Cleanup failed

---

## Complete Verification Checklist

### Folder Structure
- [ ] `C:\hls\data\1\360p\` exists
- [ ] `C:\hls\data\1\720p\` exists
- [ ] `C:\hls\data\1\master.m3u8` exists
- [ ] No segments in `C:\hls\data\1\` (root)

### Files
- [ ] `360p/playlist.m3u8` exists
- [ ] `360p/segment_*.ts` files exist (>= 1)
- [ ] `720p/playlist.m3u8` exists
- [ ] `720p/segment_*.ts` files exist (>= 1)
- [ ] No .ts files in root

### Content
- [ ] Master playlist references `360p/playlist.m3u8`
- [ ] Master playlist references `720p/playlist.m3u8`
- [ ] 360p playlist references `segment_*.ts`
- [ ] 720p playlist references `segment_*.ts`

### Size
- [ ] 360p folder size < 720p folder size
- [ ] Both folders have content

### Playback
- [ ] 360p stream plays in FFmpeg or VLC
- [ ] 720p stream plays in FFmpeg or VLC
- [ ] Master playlist plays with both options

### Database
- [ ] API returns both 360p and 720p
- [ ] Correct paths stored in database

---

## Troubleshooting

### Issue: Only 360p or 720p folder exists
**Cause**: Overwrite still occurring or second encoding failed
**Solution**: Check FFmpeg command in logs

### Issue: Same file size for 360p and 720p
**Cause**: Both are same quality (encoding settings wrong)
**Solution**: Verify bitrate parameters in buildFFmpegCommand

### Issue: Playback fails
**Cause**: Playlist paths wrong or segments missing
**Solution**: Verify folder structure with tree command

### Issue: Segments mixed in root folder
**Cause**: Fix not applied or reverted
**Solution**: Verify FFmpeg command uses `outputDir` not `movieDir`

---

## Performance Expectations

**For 2-minute video with 2 qualities (360p + 720p)**:
- CPU encoding: 10-30 minutes
- GPU encoding: 1-3 minutes
- Total processing time: 10-30 minutes (both qualities)

---

**Status**: ✅ Testing guide ready
