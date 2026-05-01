# Tóm Tắt Các Lỗi và Khắc Phục (2026-05-01)

## 🔴 Vấn Đề 1: HTTP 502 Khi Xem Phim

### Triệu Chứng
```
Exception in thread "JFXMedia HLS Playlist Thread" 
com.sun.media.jfxmedia.MediaException: HTTP responce code: 502
```

### Nguyên Nhân
- **GPU encoding (NVENC) fail**: NVIDIA driver quá cũ (< 570.0)
- **FFmpeg crash**: Khi retry CPU encoding, xử lý video quá lâu hoặc crash
- **Server không phản hồi**: Do xử lý async video bị lỗi

### Giải Pháp ✅ Đã Áp Dụng
```yaml
ffmpeg:
  use-cuda: false              # Tắt GPU encoding
  segment-duration: 6
```

**Files đã cập nhật:**
- `hls-server/src/main/resources/application.yml`
- `hls-server/src/main/resources/application-server-a.yml`
- `hls-server/src/main/resources/application-server-b.yml`

**Build status**: ✅ `mvn clean package` hoàn tất

**Khi nào có hiệu lực**: Sau khi khởi động server với JAR files mới

---

## 🔴 Vấn đề 2: Không Thể Comment Vào Phim - Lỗi HTML 502

### Triệu Chứng
```
Exception in thread "..." 
com.sun.media.jfxmedia.MediaException: HTML response instead of JSON
```

### Nguyên Nhân
- Endpoint `/reviews/summary` không phản hồi từ server (chưa rebuild)
- Nginx trả về 502 Bad Gateway vì backend không sẵn sàng

### Giải Pháp ✅ Đã Áp Dụng
- Rebuild server với config CUDA disabled
- Endpoint đã có trong `MovieReviewController`:
  ```java
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<MovieRatingSummaryResponse>> 
    getMovieRatingSummary(@PathVariable Long movieId)
  ```

**Fix**: Khởi động server mới → comment phim sẽ hoạt động

---

## 🔴 Vấn Đề 3: Slot Quản Lý Client Sai - Cả 2 Servers Có Cùng Client

### Triệu Chứng
- Cả Server A và Server B đều track **cùng 1 client**
- Giải pháp: Mỗi server chỉ được 2 clients, vậy tổng cộng 4 clients
- **Thực tế**: Sau client thứ 2, tất cả request khác bị reject (HTTP 503)

### Root Cause (Chi Tiết Kỹ Thuật)

```
Client A xem phim:
  ├─ Request tới Nginx (port 8080)
  ├─ Nginx Random chọn Server B
  ├─ Server B track Client A trong LoadSimulationInterceptor.activeViewers
  └─ Server B: activeViewers = {client-A: timestamp}

Client A request tiếp theo (lấy segment):
  ├─ Request tới Nginx (port 8080)
  ├─ Nginx Random chọn Server A (khác Server B)
  ├─ Server A track Client A trong LoadSimulationInterceptor.activeViewers
  └─ Server A: activeViewers = {client-A: timestamp}

KẾT QUẢ:
  ├─ Server A đánh vào: "Server A có 1 client"
  ├─ Server B đánh vào: "Server B có 1 client"
  └─ ❌ Vấn đề: Cùng 1 client được track 2 lần!
```

**Hệ quả**: 
- Global slots = 2 + 2 = 4 (đúng)
- Nhưng nếu Client B xem phim, có thể:
  - Server A lại nhận Client B, rejeck vì slot full (ClientA + ClientB)
  - Server B lại nhận Client A, accept vì slot có sẵn
  - Kết quả: Cùng 1 client được track 2 lần, slots bị lãng phí!

### Giải Pháp ✅ Đã Áp Dụng

**Thêm `ip_hash` vào Nginx** → **Sticky Session**

```nginx
upstream hls_backend {
    ip_hash;  # ← THÊM DÒNG NÀY
    server host.docker.internal:8081 max_fails=0;
    server host.docker.internal:8082 max_fails=0;
}
```

**Cách hoạt động:**
- Client với IP 192.168.1.100 luôn đi tới Server A
- Client với IP 192.168.1.101 luôn đi tới Server B
- **Kết quả**: Mỗi client chỉ track 1 lần

**File đã cập nhật:**
- `infra/nginx/nginx.conf` - Thêm `ip_hash;`

**Status**: ✅ Nginx restarted thành công


## 🔴 Vấn Đề 4: IPv6 Resolution Bug - Nginx Không Thể Kết Nối Tới Servers

### Triệu Chứng
```
WARNING: onError, errCode=0, msg=could not connect to media 
http://localhost:8080/api/hls/1/360p/playlist.m3u8?userEmail=...
[error] connect() to [fdc4:f303:9324::254]:8082 failed (101: Network unreachable)
```

### Root Cause 🔍
**Vấn đề**: Nginx resolve `host.docker.internal` thành **IPv6 link-local address** (`fdc4:f303:9324::254`), nhưng servers chỉ listen trên IPv4 (`192.168.65.254`)

**Kết quả**:
```
Client 3 xem phim:
  ├─ ip_hash → định tuyến tới Server B (8082)
  ├─ Nginx cố kết nối tới [fdc4:f303:9324::254]:8082 (IPv6)
  ├─ ❌ Network unreachable → 502 error
  ├─ Nginx retry tới 8081 → ✅ 200 OK  
  └─ ❌ Nhưng client vẫn thấy 502 từ first attempt
  
**Hậu quả**: 
- Client 3 stuck, không thể xem phim
- Chỉ khi dừng phim ở Server B thì client 3 mới chạy được
- **Vấn đề gốc**: sticky session chỉ định client 3 tới Server B, nhưng Server B fail (IPv6), client không biết retry
```

### Giải Pháp ✅ Đã Áp Dụng

**Thay `host.docker.internal` bằng explicit IPv4 address** `192.168.65.254`

```nginx
upstream hls_backend {
    ip_hash;
    server 192.168.65.254:8081 max_fails=0;  # ← IPv4 explicit
    server 192.168.65.254:8082 max_fails=0;  # ← IPv4 explicit
}
```

**Thêm DNS resolver config** (IPv4-only):
```nginx
resolver 127.0.0.11 ipv6=off;
resolver_timeout 5s;
```

**File đã cập nhật:**
- `infra/nginx/nginx.conf` - Thay hostname → IPv4, thêm resolver

**Status**: ✅ Nginx restarted, logs confirm no IPv6 errors
- Logs before: `connect() to [fdc4:f303:9324::254]:8082 failed`
- Logs after: `status=200 server="8082" retry="khong"` ✅

---
## 🚀 **Hành Động Cần Làm**

### 1️⃣ Khởi Động Server Mới (với CUDA disabled)

**Option A: Dùng JAR file vừa build**
```bash
cd hls-server

# Start Server A
java -Dspring.profiles.active=server-a -jar target/hls-server-0.0.1-SNAPSHOT.jar

# Trong terminal khác - Start Server B
java -Dspring.profiles.active=server-b -jar target/hls-server-0.0.1-SNAPSHOT.jar
```

**Option B: Dùng IntelliJ IDE** 
- Stop servers hiện tại
- Run -> Edit Configurations
- Start Server A, Server B với profiles: `server-a`, `server-b`

**Option C: Dùng Maven (slow)**
```bash
# Terminal 1
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server-a"

# Terminal 2
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server-b"
```

### 2️⃣ Kiểm Tra Servers Chạy OK

```bash
# Test Server A
curl -s http://localhost:8081/api/movies | head -c 200

# Test Server B
curl -s http://localhost:8082/api/movies | head -c 200

# Test through Nginx (port 8080)
curl -s http://localhost:8080/api/movies | head -c 200
```

### 3️⃣ Test Các Tính Năng

✅ **Test Thêm Phim**: Thêm phim mới với `processingMinutes > 0`
- ✅ Phim được tạo thành công (status = PROCESSING)
- ✅ Server xử lý video bằng CPU encoder (không crash)
- ✅ Phim chuyển sang PUBLISHED sau khi hoàn tất

✅ **Test Xem Phim**: Click "Watch" trên phim
- ✅ Không bị lỗi HTTP 502
- ✅ Video stream OK
- ✅ Có thể comment/rate

✅ **Test Slot Limit**: Mở 4+ clients xem phim
- ✅ Client 1-4: Xem thành công
- ✅ Client 5+: Bị reject (HTTP 503 "Server viewer capacity reached")
- ✅ Slot được phân chia: Server A có 2, Server B có 2 (không duplicate)

---

## 📊 **Kiến Thức Chuyên Sâu**

### Vấn Đề GPU Encoding
```
NVIDIA NVENC yêu cầu driver >= 570.0
Bạn có driver < 570.0 → GPU encoding FAIL
→ Fallback CPU encoding (libx264) rất chậm
→ Video processing timeout hoặc crash
→ Server không phản hồi → 502 Bad Gateway
```

**Giải Pháp long-term**: Cập nhật NVIDIA driver lên 570.0+

### Vấn Đề Sticky Session
```
Nginx load balancing mặc định: Random (Round-robin hoặc Least connections)
→ Cùng 1 client có thể request tới Server A, rồi Server B
→ Mỗi server track riêng → Slot bị duplicate

Giải pháp: ip_hash
→ Client từ 192.168.1.100 LUÔN đi tới Server A
→ Cùng client chỉ track 1 lần
→ Slots được dùng hiệu quả: 2 + 2 = 4 thực sự
```

### Quản Lý Slot (LoadSimulationInterceptor)
```
Per-server:
- Server A: max-active-per-server = 2 → activeViewers.size() <= 2
- Server B: max-active-per-server = 2 → activeViewers.size() <= 2
- Total: 4 slots

Config:
  app.simulation.viewer.max-active-per-server: 2     (per-server limit)
  app.simulation.viewer.ttl-seconds: 20               (session timeout)
  app.simulation.viewer.cleanup-interval-ms: 5000    (cleanup check interval)
```

---

## ✅ **Checklist Hoàn Tất**

- [x] Disable GPU encoding (CUDA)
- [x] Build server với config mới
- [x] Add `ip_hash` vào Nginx (sticky session)
- [x] Fix IPv6 resolution bug - explicit IPv4 address
- [x] Restart Nginx - confirm no errors
- [ ] **TODO**: Khởi động servers mới (JAR với CUDA disabled)
- [ ] **TODO**: Test 3 clients xem phim (verify slot management)
- [ ] **TODO**: Confirm tất cả features work

---

## 🆘 **Nếu Còn Lỗi**

### Vẫn bị HTTP 502 khi xem phim?
1. Check log server: `.demo-logs/server-a.log`, `.demo-logs/server-b.log`
2. Tìm ERROR hoặc Exception
3. Nếu thấy "FFmpeg" error → FFmpeg process crash
4. Kiểm tra file video tồn tại không?

### Comment phim vẫn bị lỗi HTML?
1. Rebuild server: `mvn clean package`
2. Restart servers
3. Test endpoint trực tiếp: `curl http://localhost:8081/api/movies/1/reviews/summary`

### Slot vẫn sai (cùng client track 2 lần)?
1. Check Docker logs: `docker logs hls-lb`
2. Verify `ip_hash` được load: `docker inspect hls-lb | grep -i nginx.conf`
3. Restart Nginx: `docker restart hls-lb`

---

## 📞 **Liên Hệ**

Nếu cần hỗ trợ thêm, check:
- [TROUBLESHOOTING_DESKTOP_CLIENT.md](./TROUBLESHOOTING_DESKTOP_CLIENT.md)
- [hls-server/MONITOR_README.md](./hls-server/MONITOR_README.md)
