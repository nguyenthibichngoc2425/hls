# UPGRADE PLAN TO MULTI-SERVER HLS

## 1. Mục tiêu

Nâng cấp hệ thống backend từ 1 server thành kiến trúc 2 server phía sau load balancer, dùng chung 1 database, và giữ nguyên logic HLS hiện tại.

Tai lieu nay duoc cap nhat theo trang thai hien tai: ban test multi-server da duoc trien khai va chay duoc.

---

## 2. Kiến trúc mục tiêu (Phase 1 – test)

### 2.1 Cấu hình yêu cầu

* Server A: chạy backend port 8081
* Server B: chạy backend port 8082
* Cả 2 server dùng chung database
* Load balancer đứng trước để phân phối request
* Monitor Swing tren ca A/B doc chung log tu DB

---

### 2.2 Sơ đồ kiến trúc

```text
                 +-----------------------------+
                 |      Desktop Client         |
                 |  (Swing + JavaFX + hls.js) |
                 +--------------+--------------+
                                |
                                | HTTP /api/* và /api/hls/*
                                v
                    +-------------------------+
                    |      Load Balancer      |
                    |   (Nginx/Reverse Proxy) |
                    +-----------+-------------+
                                |
              +-----------------+-----------------+
              |                                   |
              v                                   v
    +--------------------+             +--------------------+
    |  HLS Server A      |             |  HLS Server B      |
    |  Spring Boot:8081  |             |  Spring Boot:8082  |
    +---------+----------+             +----------+---------+
              |                                   |
              +-----------------+-----------------+
                                |
                                v
                     +----------------------+
                     | Shared Relational DB |
                     | PostgreSQL / MySQL   |
                     +----------------------+

        HLS storage phải được chia sẻ giữa các server (rất quan trọng)
```

---

## 3. Luồng request sau khi có load balancer

### 3.1 Luồng API

1. Client gọi URL của load balancer
2. Load balancer chuyển request đến server A hoặc B
3. Server xử lý request và truy cập database chung
4. Response trả về client

---

### 3.2 Luồng HLS

1. Client request master playlist
2. Request có thể vào server A
3. Sau đó client request playlist và segment
4. Các request tiếp theo có thể vào server B
5. Video chỉ chạy ổn nếu cả 2 server đều có cùng dữ liệu HLS

👉 Kết luận:

* File HLS phải được **chia sẻ hoặc đồng bộ hoàn toàn giữa các server**

---

## 4. Stateless và session

* API hiện tại gần như stateless → phù hợp load balancing

* Không cần sticky session nếu:

  * dùng chung DB
  * dùng chung storage

* userEmail vẫn hoạt động nếu monitoring được tập trung

👉 Khuyến nghị:

* Giữ hệ thống stateless
* Không phụ thuộc vào 1 server cụ thể

---

## 5. Kế hoạch triển khai từng bước

### Phase 1: Chạy 2 server + DB chung

* Chạy 2 backend (8081, 8082)
* Cấu hình cùng DB
* Test API riêng từng server

Trang thai: **Da hoan thanh (demo)**

Checklist:

* Login/register OK
* Danh sách phim giống nhau
* Favorites/history đồng bộ

---

### Phase 2: Thêm load balancer

* Cấu hình reverse proxy (Nginx)
* Route request đến 2 server

Trang thai: **Da hoan thanh (demo)**

Checklist:

* Gọi API qua balancer OK
* Nhiều user truy cập được
* Không lỗi route

---

### Phase 3: Test HLS multi-server

* Đảm bảo cả 2 server đọc được file HLS
* Test request qua 2 server khác nhau

Trang thai: **Da hoan thanh co dieu kien**

Checklist:

* Không lỗi 404 segment
* Video không bị giật khi đổi server
* Range request hoạt động đúng

---

### Phase 4: Giả lập lỗi

Mục tiêu: kiểm tra chịu lỗi

Cách làm:

* Random fail request (10%–30%)
* Tắt server A/B luân phiên
* Giả lập server chậm

Trang thai: **Da hoan thanh (demo random failure + retry)**

Kết quả mong đợi:

* Load balancer tự chuyển sang server còn sống
* Video vẫn chạy
* API vẫn hoạt động

---

## 6. Các vấn đề khi chạy multi-server

### 6.1 Đồng bộ playlist và segment

Rủi ro:

* Server này có file, server kia không

Giải pháp:

* Dùng shared storage (SMB/NFS)
* Đảm bảo cấu trúc file giống nhau
* Chỉ publish khi file đã đầy đủ

---

### 6.2 Cache

Rủi ro:

* Playlist và segment không đồng bộ

Giải pháp:

* m3u8: không cache
* ts: cache lâu
* đồng bộ cache giữa server

---

### 6.3 Lỗi khi xử lý video

Rủi ro:

* Server A xử lý xong nhưng B chưa có file

Giải pháp:

* Kiểm tra file trước khi publish
* Thêm bước validate sau encode

---

### 6.4 Monitoring

Rủi ro:

* Mỗi server 1 màn hình → không thấy tổng

Giải pháp:

* Da gop log ve 1 noi bang shared PostgreSQL (`system_logs`)
* Monitor A/B deu doc cung nguon log DB
* Online viewers van theo tung server (chu y khi demo)

---

## 7. Thiết kế load balancer

Yêu cầu tối thiểu:

* Load balancing (round-robin)
* Health check
* Retry khi lỗi
* Forward IP (X-Forwarded-For)

Chiến lược:

* Bắt đầu với round-robin
* Không cần sticky session
* Retry khi gap timeout/5xx

---

## 8. Rủi ro

* Segment không tồn tại trên server khác
* Cache lệch
* Lỗi đồng bộ khi xử lý
* Server chỉ có API nhưng thiếu file
* Range request không đồng nhất
* Online viewers chua tong hop global

---

## 9. Tiêu chí hoàn thành

### Functional:

* API chạy qua load balancer OK
* Video chạy khi request qua 2 server
* 1 server chết vẫn dùng được
* Structured log hien thi ro server/event/user/ip/endpoint

---

### Operational:

* Biết server nào đang hoạt động
* Failover hoạt động đúng
* DB không lỗi

---

## 10. Hướng nâng cấp sau

* CDN
* Auto scale
* Monitoring viewers tong hop toan cum
* Cache phân tán
* Tracing

---

## 11. Quyết định quan trọng cho production

👉 Phải chọn cách lưu HLS:

* Shared storage (SMB/NFS)
* hoặc sync file giữa server

👉 Nếu không có cái này:

* multi-server sẽ lỗi ngay

---

## Kết luận

Multi-server HLS không khó ở load balancer, mà khó ở **storage và đồng bộ file**.

Nếu giải quyết được storage → hệ thống sẽ chạy ổn.

Trong ban demo hien tai, phan A/B + LB + shared DB log + monitor table/filter/mau da dat muc tieu trinh bay.
