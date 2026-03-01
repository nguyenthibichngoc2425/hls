# HTTP Live Streaming (HLS) - Bối cảnh dự án (chi tiết)

Tài liệu này tổng hợp đầy đủ bối cảnh dự án cho người mới: kiến trúc, giao thức HLS,
luồng xử lý video, cấu trúc lưu trữ, backend, client desktop, cấu hình và các thành phần chính.
Mục tiêu là đọc xong có thể hiểu dự án làm gì, dữ liệu ở đâu và HLS được tạo/stream như thế nào.

---

## 1. Tổng quan

Dự án **HTTP-Live-Streaming** là hệ thống streaming video theo chuẩn HLS:
- Backend (Spring Boot) cung cấp REST API, xử lý video, phục vụ HLS.
- HLS được tạo bằng FFmpeg (multi-bitrate + playlists + segments).
- Client desktop (Swing + JavaFX WebView) phát HLS bằng hls.js.
- Lưu trữ HLS ở filesystem theo `movieId/quality/segments`.

Cấu trúc repo:
- `hls-server/`: backend API, xử lý video, monitor nội bộ.
- `client-desktop/`: ứng dụng desktop phát video.

---

## 2. Phạm vi chức năng

- Quản lý phim, thể loại, người dùng.
- Đăng ký/đăng nhập.
- Tạo HLS từ video gốc (360p/720p).
- Phục vụ HLS qua HTTP (master/playlist/segment).
- Theo dõi hoạt động login & streaming bằng monitor nội bộ.
- Client desktop phát video và chọn chất lượng.

---

## 3. Kiến trúc hệ thống

```
[Client Desktop] <---- HTTP ----> [Spring Boot HLS Server]
        |                                 |
        |                                 |-- PostgreSQL (metadata)
        |                                 |-- File Storage (HLS files)
        |                                 |-- FFmpeg/FFprobe
```

Luồng tổng quát:
1. Video gốc được xử lý bằng FFmpeg tạo HLS.
2. Server lưu file HLS trên disk và cập nhật metadata.
3. Client gọi API HLS để phát.
4. Monitor nội bộ ghi log các hoạt động.

---

## 3.1. Bổ sung kiến thức mạng (theo hướng môn Lập trình mạng)

Để người mới dễ hình dung theo mô hình OSI 7 tầng và TCP/IP:

### 3.1.1. OSI 7 tầng (tham chiếu khái niệm)
1. **Physical**: tín hiệu vật lý (không đi sâu trong dự án).
2. **Data Link**: Ethernet/Wi-Fi (không xử lý trực tiếp).
3. **Network**: IP routing (hệ điều hành đảm nhiệm).
4. **Transport**: TCP (dùng cho HTTP streaming).
5. **Session**: quản lý phiên (do HTTP/TCP + ứng dụng đảm nhiệm).
6. **Presentation**: encoding/format (video HLS, mã hóa AAC/H.264).
7. **Application**: HTTP API (Spring Boot), HLS playlist/segment.

### 3.1.2. TCP/IP trong dự án
- **TCP**: đảm bảo truyền dữ liệu ổn định và theo thứ tự cho luồng tải `.m3u8` và `.ts`.
  Ở đây không dùng UDP, vì HLS chạy trên HTTP nên mặc định là TCP.
- **HTTP**: giao thức ứng dụng để client gửi request lấy playlist/segment.
- **HLS**: thực chất là **HTTP GET** liên tục tới playlist và segment.

### 3.1.3. Liên hệ thực tế trong dự án
- Client gửi **HTTP GET** tới `/api/hls/...` (tầng 7).
- Request/response đi qua **TCP** (tầng 4), đóng gói IP (tầng 3).
- Server trả playlist (`.m3u8`) hoặc segment (`.ts`) qua HTTP response.
- Player đọc playlist rồi tiếp tục gọi thêm các segment => tạo luồng streaming.
- Vì dùng HTTP, có thể tận dụng cache, CDN, và proxy như các file tĩnh khác.

---

## 4. HLS trong dự án

### 4.1. Thành phần HLS
- Master playlist: `master.m3u8`
- Variant playlist: `360p/playlist.m3u8`, `720p/playlist.m3u8`
- Segments: `segment_000.ts`, `segment_001.ts`, ...

### 4.2. Segment duration
- Mặc định 6 giây, cấu hình qua `app.ffmpeg.segment-duration`.

---

## 5. Lưu trữ HLS

Đường dẫn mặc định (application.yml):
```
app:
  hls:
    storage-path: /home/nrin31266/hls-data/videos/hls
```

Cấu trúc thư mục:
```
/home/nrin31266/hls-data/videos/hls/
├── {movieId}/
│   ├── 360p/
│   │   ├── segment_000.ts
│   │   ├── segment_001.ts
│   │   └── playlist.m3u8
│   ├── 720p/
│   │   ├── segment_000.ts
│   │   ├── segment_001.ts
│   │   └── playlist.m3u8
│   └── master.m3u8
```

Ý nghĩa:
- Mỗi movie có thư mục riêng theo ID.
- Mỗi chất lượng có playlist và segments riêng.
- Master playlist trỏ tới playlist con.

---

## 6. Backend: cấu trúc module `hls-server`

### 6.1. Packages
`hls-server/src/main/java/com/rin/hlsserver/`:
- `controller/`: REST API.
- `service/`: nghiệp vụ, FFmpeg, auth.
- `model/`: entity JPA.
- `repository/`: Spring Data.
- `dto/`: request/response.
- `monitor/`: monitor GUI.
- `exception/`: xử lý lỗi.
- `config/`: cấu hình.

---

## 7. HLS Streaming Controller

File: `hls-server/src/main/java/com/rin/hlsserver/controller/HlsStreamingController.java`

### 7.1. Master playlist
```
GET /api/hls/{movieId}/master.m3u8
```
- Kiểm tra movie tồn tại và đã xử lý.
- Đọc file master playlist.
- Có thể append `userEmail` vào URL.
- Trả về `application/vnd.apple.mpegurl`.

### 7.2. Variant playlist
```
GET /api/hls/{movieId}/{quality}/playlist.m3u8
```
- Chấp nhận `360p` hoặc `720p`.
- Đọc playlist trong thư mục quality.
- Append `userEmail` vào segment URLs.

### 7.3. Segment
```
GET /api/hls/{movieId}/{quality}/{segmentName}
```
- `segmentName` dạng `segment_\d{3}.ts`.
- Trả về `video/mp2t`.
- Hỗ trợ Range request (chunk ~1MB).

### 7.4. Ready check
```
GET /api/hls/{movieId}/ready
```
- Trả `true/false` nếu movie đã publish và có master playlist.

---

## 8. FFmpeg xử lý video

File: `hls-server/src/main/java/com/rin/hlsserver/service/FFmpegService.java`

### 8.1. Luồng xử lý
1. Kiểm tra file source.
2. Tạo thư mục output `hlsStoragePath/{movieId}`.
3. Lấy thông tin video bằng `ffprobe`.
4. Tạo HLS cho từng chất lượng (360p, 720p).
5. Tạo `master.m3u8`.
6. Trả về danh sách `VideoQuality`.

### 8.2. Chất lượng mặc định
- 360p: 640x360, ~800 kbps.
- 720p: 1280x720, ~2500 kbps.

### 8.3. CUDA / CPU
- Dùng `h264_nvenc` khi `app.ffmpeg.use-cuda=true`.
- Dùng `libx264` khi `false`.

### 8.4. Segment duration
- `-hls_time` theo config (mặc định 6s).

---

## 9. Models dữ liệu chính

### 9.1. Movie
File: `hls-server/src/main/java/com/rin/hlsserver/model/Movie.java`
- `title`, `description`, `imageUrl`
- `sourceVideoPath`: video gốc
- `masterPlaylistPath`: đường dẫn master
- `duration`, `processingMinutes`
- `status`: DRAFT / PROCESSING / PUBLISHED / FAILED
- `processingProgress`
- `videoQualities` (1-n)

### 9.2. VideoQuality
File: `hls-server/src/main/java/com/rin/hlsserver/model/VideoQuality.java`
- `quality`: Q360P/Q720P
- `playlistPath`
- `segmentsPath`
- `bitrate`, `resolution`, `fileSize`

### 9.3. User / Role / Genre
- `User`: email, passwordHash, fullName, roles.
- `Role`: name.
- `Genre`: genreId (EN), name (VI), description.

### 9.4. FavoriteMovie / WatchHistory
- `FavoriteMovie`: user ↔ movie (yêu thích).
- `WatchHistory`: user ↔ movie (lịch sử xem).

### 9.5. VideoProcessingTask
- Theo dõi trạng thái xử lý video (PENDING/RUNNING/COMPLETED/...)
- Lưu progress, error, started/completed time.

---

## 10. Auth & User

### 10.1. AuthController
File: `hls-server/src/main/java/com/rin/hlsserver/controller/AuthController.java`
- `POST /api/auth/register`
- `POST /api/auth/login`

### 10.2. JWT
- `app.jwt.signerKey` chỉ **được cấu hình sẵn**, hiện tại **chưa dùng cho streaming**.
- Nếu triển khai auth cho API đăng nhập/đăng ký thì có thể dùng khóa này, còn luồng HLS hiện chạy qua HTTP GET bình thường.

---

## 11. API chức năng phim

Các controller chính:
- `MovieController`: CRUD phim, xử lý video.
- `GenreController`: quản lý thể loại.
- `UserController`: quản lý người dùng.
- `FavoriteMovieController`: phim yêu thích.
- `WatchHistoryController`: lịch sử xem.

---

## 12. Monitor nội bộ

File: `hls-server/MONITOR_README.md`

Chức năng:
- Log login success/fail.
- Log HLS master/playlist/segment.
- Theo dõi user đang xem (sessions in-memory).

Đặc điểm:
- GUI Swing mở cùng server.
- Ring buffer 2000 log entries.
- Session timeout mặc định 10-15s.

---

## 13. Database

Cấu hình trong `application.yml`:
- `spring.datasource.*` (PostgreSQL).
- `spring.jpa.hibernate.ddl-auto=update`.

Entities chính:
- Movie, VideoQuality, User, Role, Genre,
  FavoriteMovie, WatchHistory, VideoProcessingTask.

---

## 14. Client Desktop

### 14.1. Tổng quan
- Module: `client-desktop/`.
- Swing UI + JavaFX WebView.
- Player HTML dùng hls.js.

### 14.2. VideoPlayerForm
File: `client-desktop/src/main/java/raven/modal/demo/forms/VideoPlayerForm.java`
- Tạo HTML runtime.
- Play/pause, seek, volume, full-screen.
- Cho phép chọn chất lượng (quality menu).

### 14.3. URL playlist
```
http://localhost:8080/api/hls/{movieId}/{quality}/playlist.m3u8?userEmail=...
```
Nếu không có quality, dùng master playlist:
```
http://localhost:8080/api/hls/{movieId}/master.m3u8?userEmail=...
```

---

## 15. Cấu hình quan trọng

File: `hls-server/src/main/resources/application.yml`
- `server.port`: 8080
- `app.hls.storage-path`: nơi lưu HLS
- `app.ffmpeg.use-cuda`: bật/tắt GPU
- `app.ffmpeg.segment-duration`: độ dài segment
- `app.jwt.signerKey`: khóa JWT
- `monitor.online.timeoutSeconds`: timeout session

---

## 16. Luồng xử lý video chi tiết

1. Tạo Movie với `sourceVideoPath`.
2. Gọi xử lý video => `FFmpegService.processVideoToHLS`.
3. Tạo output folder theo movieId.
4. Chạy FFmpeg tạo `360p` và `720p`.
5. Tạo `master.m3u8`.
6. Movie được set `masterPlaylistPath`.
7. Client bắt đầu stream qua API HLS.

---

## 17. Luồng streaming chi tiết

1. Client gọi master playlist.
2. Master trả danh sách playlist theo chất lượng.
3. Client chọn quality, tải playlist.
4. Playlist trả segment `.ts`.
5. Client tải segments liên tục.
6. Monitor ghi nhận tất cả request.

---

## 18. Lưu ý vận hành

- Đảm bảo ffmpeg/ffprobe đã cài và nằm trong PATH.
- Đường dẫn `app.hls.storage-path` phải tồn tại và có quyền ghi.
- Movie phải `PUBLISHED` và có master playlist mới stream được.
- HLS segments có thể cache lâu (header cache max-age lớn).

---

## 19. File quan trọng cho dev mới

- `README.md`: mô tả storage layout HLS.
- `PROJECT_CONTEXT.md`: tài liệu tổng quan chi tiết.
- `hls-server/MONITOR_README.md`: monitor nội bộ.
- `hls-server/src/main/resources/application.yml`.
- `hls-server/src/main/java/com/rin/hlsserver/controller/HlsStreamingController.java`.
- `hls-server/src/main/java/com/rin/hlsserver/service/FFmpegService.java`.
- `client-desktop/src/main/java/raven/modal/demo/forms/VideoPlayerForm.java`.

---

## 20. Hướng dẫn nhanh chạy dự án

### 20.1. Backend
```
cd hls-server
mvn spring-boot:run
```

### 20.2. Client desktop
```
cd client-desktop
mvn package
java -jar target/*.jar
```

---

## 21. Mở rộng tương lai (gợi ý)

- Thêm chất lượng 1080p/4K.
- Signed URL hoặc DRM.
- CDN/S3 storage.
- Thống kê view.
- Adaptive bitrate nâng cao.

---

## 22. Tóm tắt endpoint chính

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### HLS
- `GET /api/hls/{movieId}/master.m3u8`
- `GET /api/hls/{movieId}/{quality}/playlist.m3u8`
- `GET /api/hls/{movieId}/{quality}/{segmentName}`
- `GET /api/hls/{movieId}/ready`

### Movie/Genre/User
- `GET/POST/PUT/DELETE /api/movies/*`
- `GET/POST/PUT/DELETE /api/genres/*`
- `GET/POST/PUT/DELETE /api/users/*`

---

## 23. Kết luận

Dự án tập trung vào HLS streaming end-to-end:
- Backend tạo + phục vụ HLS.
- Client desktop phát HLS.
- Monitor nội bộ giúp quan sát hoạt động.

Nếu có thay đổi lớn về cấu trúc, hãy cập nhật tài liệu này để người mới luôn nắm được bối cảnh dự án.