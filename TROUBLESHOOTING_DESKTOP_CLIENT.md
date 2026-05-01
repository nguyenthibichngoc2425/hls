# Hướng Dẫn Khắc Phục Sự Cố Đăng Nhập Desktop Client

## Lỗi: `JsonParseException: Unexpected character ('<')`

Lỗi này có nghĩa là desktop client đã nhận **HTML thay vì JSON** từ API. Điều này thường xảy ra khi:
1. Máy chủ backend không chạy hoặc không thể truy cập
2. Nginx load balancer không hoạt động bình thường
3. Yêu cầu bị chặn và trả về trang lỗi

---

## Danh Sách Kiểm Tra Khắc Phục Nhanh

### 1. **Xác Minh Máy Chủ Đang Chạy**

Kiểm tra xem máy chủ backend có chạy trên các cổng dự kiến không:

```bash
# Kiểm tra Server A (cổng 8081)
curl -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@gmail.com\",\"password\":\"123\"}"

# Kiểm tra Server B (cổng 8082)  
curl -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@gmail.com\",\"password\":\"123\"}"
```

Nếu bạn nhận được phản hồi JSON đúng định dạng `{"code":...}`, máy chủ đang hoạt động.

---

### 2. **Xác Minh Nginx/Docker Đang Chạy**

```bash
# Kiểm tra xem các container Docker có đang chạy không
docker ps

# Phải thấy:
# - hls-lb (Nginx load balancer trên cổng 8080)
# - hls-postgres (Cơ sở dữ liệu PostgreSQL)

# Nếu chưa chạy, khởi động:
docker-compose up -d
```

---

### 3. **Kiểm Tra Proxy của Nginx**

```bash
# Kiểm tra thông qua Nginx (cổng 8080)
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin@gmail.com\",\"password\":\"123\"}"
```

So sánh phản hồi với các kiểm tra máy chủ trực tiếp ở trên.

---

## Giải Pháp Tạm Thời: Kết Nối Trực Tiếp Đến Máy Chủ

Nếu Nginx gặp sự cố, hãy chỉnh sửa desktop client để kết nối trực tiếp đến một máy chủ:

**Chỉnh sửa**: `client-desktop/src/main/resources/app.properties`

```properties
# Ban đầu (thông qua Nginx load balancer)
# app.api-base-url=http://localhost:8080/api

# Giải pháp tạm thời - Kết nối trực tiếp đến Server A
app.api-base-url=http://localhost:8081/api

# HOẶC - Kết nối trực tiếp đến Server B
# app.api-base-url=http://localhost:8082/api
```

Sau đó xây dựng lại và chạy desktop client:
```bash
cd client-desktop
mvn clean package
java -jar target/client-desktop-*.jar
```

---

## Thông Tin Gỡ Lỗi

Desktop client đã được cập nhật với các tính năng ghi nhật ký nâng cao. Khi chạy, hãy tìm:

```
[HTTP DEBUG] POST http://localhost:8080/api/auth/login
[HTTP DEBUG] Status Code: 200
[HTTP DEBUG] Content-Type: application/json
[HTTP DEBUG] Response Preview: {"code":"0000",...}
```

Nếu bạn thấy HTML trong bản xem trước phản hồi thay vì JSON, hãy kiểm tra:
1. **Máy chủ có đang chạy không?** Sử dụng các kiểm tra curl ở trên
2. **Nginx có đang hoạt động bình thường không?** Kiểm tra Docker và nhật ký
3. **Có bức tường lửa chặn không?** Kiểm tra các quy tắc Tường Lửa Windows

---

## Gỡ Lỗi Nâng Cao

### Kiểm Tra Nhật Ký Nginx (nếu sử dụng Docker)
```bash
docker logs hls-lb
```

### Kiểm Tra Nhật Ký Máy Chủ
Xem `.demo-logs/server-a.log` hoặc `.demo-logs/server-b.log` để xem thông báo lỗi

### Kiểm Tra Định Dạng Phản Hồi JSON
Sử dụng Postman hoặc curl để xem phản hồi chính xác:
```bash
curl -v -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@gmail.com\",\"password\":\"password123\"}"
```

Cờ `-v` hiển thị tất cả các tiêu đề và phản hồi đầy đủ.

---

## Các Kịch Bản Phổ Biến

### Kịch Bản 1: "Kết Nối Bị Từ Chối"
- **Nguyên Nhân**: Không có máy chủ lắng nghe trên cổng
- **Sửa Chữa**: Khởi động máy chủ backend (Server A trên 8081, Server B trên 8082)

### Kịch Bản 2: "Nhận Được Trang Lỗi HTML"
- **Nguyên Nhân**: Nginx chạy nhưng máy chủ backend không phản hồi
- **Sửa Chữa**: 
  1. Kiểm tra xem máy chủ có chạy không
  2. Kiểm tra xem máy chủ có thể truy cập được thông qua `http://localhost:8081` và `http://localhost:8082` không
  3. Kiểm tra nhật ký Docker: `docker logs hls-lb`

### Kịch Bản 3: "Kết Nối Hết Thời Gian Chờ Cổng 8080"
- **Nguyên Nhân**: Container Docker không chạy
- **Sửa Chữa**: Khởi động Docker: `docker-compose up -d`

### Kịch Bản 4: "Kiểm Tra Curl Thành Công Nhưng Vẫn Gặp Lỗi Trong Ứng Dụng"
- **Nguyên Nhân**: Có thể là tường lửa hoặc proxy chặn các yêu cầu HTTP của Java
- **Sửa Chữa**: 
  1. Kiểm tra Tường Lửa Windows để tìm các quy tắc ứng dụng Java
  2. Thử `--add-opens java.base/java.net=ALL-UNNAMED` tham số JVM
  3. Thử kết nối đến `127.0.0.1` thay vì `localhost`

---

## Nếu Không Có Gì Hoạt Động

1. **Thu Thập Thông Tin Gỡ Lỗi**:
   - Sao chép đầu ra bảng điều khiển từ desktop client
   - Chạy các kiểm tra `curl -v` và lưu đầu ra
   - Kiểm tra các tệp `.demo-logs/server-*.log`

2. **Kiểm Tra Môi Trường**:
   - Phiên bản Java: Phải là Java 21 trở lên
   - Docker chạy: `docker --version` và `docker ps`
   - Mạng: Bạn có thể ping localhost không?

3. **Thử Khởi Động Máy Chủ Thủ Công**:
   ```bash
   cd hls-server
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server-a"
   # Trong terminal khác:
   mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=server-b"
   ```

