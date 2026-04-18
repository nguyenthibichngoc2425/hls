# HLS Multi-Server Demo (A/B + Nginx + Shared DB Log)

## 1. Tong quan

Repository nay hien tai da co ban demo multi-server cho HLS streaming:

- Backend A: Spring Boot, port `8081`
- Backend B: Spring Boot, port `8082`
- Load Balancer: Nginx, port `8080`
- Database dung chung: PostgreSQL (`hlsdb`)
- HLS storage dung chung tren may host

Muc tieu demo: nhin vao la thay ro phan phoi request A/B, failover, va monitoring chung.

## 2. Kien truc hien tai

```text
Desktop Client -> Nginx (8080) -> Server A (8081)
                               -> Server B (8082)

Server A/B -> Shared PostgreSQL (hlsdb)
Server A/B -> Shared HLS folder tren host
```

Luu y:

- `docker-compose.yml` hien chi chay `postgres` va `nginx`.
- Backend A/B chay bang process Java tren host (khong dong goi Docker).

## 3. Structured log va monitor

He thong da chuyen tu log text dai sang structured log trong bang `system_logs`.

### 3.1 Cac cot log chinh

- `server_name`
- `event_type`
- `endpoint`
- `user_email`
- `ip_address`
- `port`
- `level`
- `created_at`

### 3.2 Event type duoc chuan hoa

- `LOGIN_SUCCESS`
- `LOGIN_FAIL`
- `LOGOUT`
- `HLS_MASTER`
- `HLS_PLAYLIST`
- `HLS_SEGMENT`
- `API_REQUEST`
- `ERROR`

### 3.3 Monitor Swing

Monitor tren ca A va B doc cung mot nguon log DB, header hien thi:

- `SERVER-X | IP:PORT | SHARED DB MODE`
- `Nguon log: DATABASE (SHARED)`

Tab log hien thi dang bang cot:

- `Time | Server | Event | User | IP | Endpoint`

Co mau theo event:

- Login/Logout: xanh la
- HLS: xanh duong
- Error: do

Co filter nhanh:

- `ALL | LOGIN | HLS | ERROR`

## 4. Chay demo nhanh

Xem huong dan day du tai `RUN.md`.

Lenh nhanh:

```bash
bash run-multi-server-demo.sh
```

Dung demo:

```bash
bash stop-multi-server-demo.sh
```

## 5. Thu muc chinh

- `hls-server/`: backend va monitor Swing
- `client-desktop/`: desktop app
- `modal-dialog/`: thu vien UI dung chung
- `infra/nginx/nginx.conf`: cau hinh load balancer
- `run-multi-server-demo.sh`: script start demo A/B + infra
- `stop-multi-server-demo.sh`: script stop demo
- `RUN.md`: runbook chi tiet
- `system-design/`: tai lieu phan tich va ke hoach
