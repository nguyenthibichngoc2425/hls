# SYSTEM ANALYSIS

## 1. Phạm vi hệ thống và kiến trúc hiện tại

Repository này chứa một hệ thống streaming video sử dụng HLS, gồm 3 module chính:

* hls-server: Backend Spring Boot xử lý metadata phim, xác thực, streaming HLS, xử lý video bằng FFmpeg và monitoring nội bộ.
* client-desktop: Ứng dụng desktop Java (Swing + JavaFX WebView + hls.js) để duyệt và phát video.
* modal-dialog: Thư viện UI dùng chung cho client.

### Triển khai hiện tại:

* 2 backend server chạy local process:

  * SERVER-A: 8081
  * SERVER-B: 8082
* 1 load balancer Nginx: 8080
* 1 database quan hệ dùng chung: PostgreSQL
* docker-compose chạy postgres + nginx
* File HLS đọc từ shared folder trên host

---

## 2. Tổng quan kiến trúc

### 2.1 Kiến trúc logic

* Presentation Layer:

  * UI desktop, form, control phát video, admin view
* Application Layer:

  * REST API: auth, phim, thể loại, user, yêu thích, lịch sử xem, HLS
* Service Layer:

  * Xử lý nghiệp vụ: auth, vòng đời phim, FFmpeg, HLS, tracking
* Persistence Layer:

  * Spring Data JPA + migration SQL
* Media Layer:

  * Pipeline FFmpeg/ffprobe tạo HLS (playlist + segment)
* Monitoring Layer:

  * Structured log lưu DB dùng chung + giao diện Swing

---

### 2.2 Runtime

#### Client:

* Gọi REST API để lấy dữ liệu phim
* Tạo URL HLS và phát bằng WebView + hls.js
* Gửi request favorites / watch-history
* Gửi thêm userEmail để tracking

#### Server:

* CRUD dữ liệu
* Encode video async (360p, 720p)
* Serve playlist + segment
* Tracking user và hiển thị qua monitor

#### Database:

* Lưu user, role, phim, chất lượng video, task xử lý, favorites, history

---

## 3. Cấu trúc project

### 3.1 Root

* pom.xml: multi-module (chỉ cho client + modal-dialog)
* hls-server: project riêng
* docker-compose.yml: chạy PostgreSQL + Nginx
* README.md: mô tả hệ thống

---

### 3.2 client-desktop

Chức năng:

* Đăng nhập / đăng ký
* Duyệt phim
* Phát video (fullscreen, đổi chất lượng)
* Favorites + lịch sử xem
* Phân quyền USER / ADMIN

Chi tiết:

* API base config trong app.properties
* Dùng Java HttpClient
* HLS URL:

  * /api/hls/{movieId}/{quality}/playlist.m3u8
  * fallback: /master.m3u8
* UI phát video dùng HTML + hls.js trong WebView

---

### 3.3 hls-server

Chức năng:

* REST API:

  * /api/auth
  * /api/movies
  * /api/genres
  * /api/users
  * /api/favorites
  * /api/watch-history
  * /api/hls
* Encode video bằng FFmpeg
* Serve file .m3u8 và .ts (có hỗ trợ Range)
* Tracking user

Hạ tầng:

* JPA + entity
* Script SQL
* Tạo admin mặc định

---

### 3.4 modal-dialog

* Thư viện UI dùng lại
* Không liên quan streaming
* Cung cấp modal, toast, animation

---

## 4. Backend (Spring Boot)

### 4.1 Layer

* Controller:

  * Nhận request → gọi service
  * HlsStreamingController xử lý streaming
* Service:

  * AuthService, MovieService, FFmpegService, HlsStreamingService...
* Repository:

  * Spring Data JPA
* Model:

  * User, Movie, VideoQuality...
* Exception:

  * Dùng RestControllerAdvice

---

### 4.2 Security

Hiện tại:

* permitAll → mở toàn bộ API
* Tắt CSRF
* JWT có code nhưng chưa dùng
* Role chưa enforce thật

=> hệ thống gần như không có bảo mật

---

## 5. Luồng HLS

### 5.1 Xử lý video

1. Tạo phim
2. Nếu cần xử lý → trạng thái PROCESSING
3. FFmpeg đọc duration
4. Encode:

   * 360p
   * 720p
5. Tạo:

   * playlist.m3u8
   * segment .ts
6. Tạo master.m3u8
7. Update DB → PUBLISHED

---

### 5.2 Luồng phát

1. Client gọi API phim
2. Kiểm tra có thể phát
3. Tạo URL HLS
4. Request playlist
5. Server đọc file, trả về
6. Client request segment
7. Server trả .ts (có thể 206)
8. hls.js tự đổi chất lượng

---

### 5.3 Adaptive Bitrate

* Nhiều chất lượng (360p, 720p)
* Master playlist chứa metadata
* hls.js tự chọn

---

## 6. Lưu trữ video

* Lưu local filesystem
* Cấu trúc:

{storage}/{movieId}/
master.m3u8
360p/
720p/

* Segment dạng: segment_001.ts
* Playlist: không cache
* Segment: cache lâu

---

## 7. Client - Server

### 7.1 API

* Auth
* Phim
* Favorites / history
* Admin

---

### 7.2 Playback

* Gọi API → lấy info
* Gọi HLS trực tiếp
* Có userEmail để tracking
* Update history khi mở phim

---

## 8. Monitoring

* Không có web dashboard
* Dùng Swing GUI nội bộ

### 8.1 Cách hoạt động

* Chạy cùng backend
* Dùng:

  * SystemLogService (DB shared)
  * OnlineWatchingStore (memory)
* Hook vào:

  * login
  * streaming
  * api request
  * error simulation
* Cleanup session
* UI refresh liên tục

---

### 8.2 Tracking

* Login
* Streaming
* User đang xem

---

### 8.3 Vấn đề

* Log da duoc giai quyet bang DB shared
* Tab nguoi dang xem van la memory theo tung server (phu hop demo nhe)

---

## 9. Data model

* User, Role
* Movie
* Genre
* VideoQuality
* Favorite
* WatchHistory

---

## 10. Hạn chế

### 10.1 Kiến trúc

* Chua co container hoa backend
* Chua co healthcheck nang cao cho backend

### 10.2 Config

* path Windows
* FFmpeg config lệch

### 10.3 Security

* mở toàn bộ API
* admin mặc định

### 10.4 Streaming

* không có shared storage
* dễ lỗi multi-server

### 10.5 Monitoring

* Log da tap trung qua DB
* Online viewers chua tong hop global theo cum server

### 10.6 Build

* hls-server không nằm trong root build

---

## 11. Khả năng nâng cấp multi-server

### Điểm tốt:

* API stateless
* DB dùng chung
* HLS path rõ ràng

### Cần cải thiện:

* Shared storage production-grade (NFS/object storage)
* Cache
* Monitoring viewers tong hop multi-server
* Security
* Load balancer healthcheck chuyen sau
