# Multi-Server HLS Demo Run Guide

This guide runs a demo setup with:

- Backend A: Spring Boot on port 8081 (non-docker process)
- Backend B: Spring Boot on port 8082 (non-docker process)
- Shared PostgreSQL (Docker)
- Nginx load balancer (Docker) on port 8080
- Shared HLS folder on the same machine
- Structured logs stored in shared PostgreSQL

## 1. Prerequisites

- Docker + Docker Compose
- Java 21
- Network access for Maven dependency download (first run)

## 2. Start All Components (one command)

From project root:

```bash
bash run-multi-server-demo.sh
```

Optional env overrides:

```bash
APP_SIMULATION_FAILURE_RATE=0.2 HLS_STORAGE_PATH=/tmp/hls-data/videos/hls bash run-multi-server-demo.sh
```

Notes:

- `APP_SIMULATION_FAILURE_RATE` controls random 500 responses on each backend.
- Both backends use the same DB and same HLS storage path.
- Client should call `http://localhost:8080/api/...` (through load balancer).
- Startup script waits until both backend ports are reachable before printing completion.

## 3. Endpoints

- Load balancer: `http://localhost:8080`
- Backend A direct: `http://localhost:8081`
- Backend B direct: `http://localhost:8082`

## 4. Verify Load Balancing and Failover

Basic API test through LB:

```bash
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "req=$i status=%{http_code}\n" http://localhost:8080/api/movies
done
```

HLS test through LB (example movie id):

```bash
curl -i "http://localhost:8080/api/hls/1/master.m3u8"
```

With failure simulation enabled, some requests return 500 from one backend.
Nginx is configured to retry to the other backend on timeout/5xx.

## 5. Logs and Monitoring

### 5.1 Load balancer logs (important)

```bash
docker compose logs -f nginx
```

Log format is prefixed with `[LB]` and includes:

- incoming request (`req`, `uri`)
- routed upstream (`upstream_addr`)
- upstream status list (`upstream_status`)
- total request status (`status`)

How to read retry:

- `upstream_addr="host.docker.internal:8081, host.docker.internal:8082"`
- `upstream_status="500, 200"`

This means first attempt failed on server A and was retried successfully on server B.

### 5.2 Backend logs

- Server A: `.demo-logs/server-a.log`
- Server B: `.demo-logs/server-b.log`

You will see random-failure logs like:

```text
[SERVER-A][SIM] Giả lập lỗi 500 tại /api/movies (rate=0.2)
```

### 5.3 Structured logs in DB (shared)

Check recent structured logs:

```bash
docker exec hls-postgres psql -U root -d hlsdb -c "select server_name, event_type, endpoint, user_email, ip_address, port, level from system_logs order by created_at desc limit 20;"
```

Expected event types include:

- `API_REQUEST`
- `LOGIN_SUCCESS`, `LOGIN_FAIL`, `LOGOUT`
- `HLS_MASTER`, `HLS_PLAYLIST`, `HLS_SEGMENT`
- `ERROR`

### 5.4 Swing Monitor (shared logs)

- Monitor on both server A and server B reads logs from shared PostgreSQL.
- Monitor header shows: `Nguồn log: DATABASE (SHARED)`.
- Both monitors should show the same latest log records (shared DB source).
- Logs tab uses table columns: `Time | Server | Event | User | IP | Endpoint`.
- Event filter is available: `ALL | LOGIN | HLS | ERROR`.
- Event color coding:
  - LOGIN/LOGOUT: green
  - HLS: blue
  - ERROR: red

### 5.5 Viewer tab (Nguoi Dang Xem)

- Shows active viewers tracked on the current server instance.
- Header and counter clearly indicate the current server (A or B).

## 6. Stop Demo

```bash
bash stop-multi-server-demo.sh
```

This stops:

- backend A process
- backend B process
- nginx container
- postgres container

## 7. Files Added/Changed

- `docker-compose.yml`
- `infra/nginx/nginx.conf`
- `hls-server/src/main/resources/application-server-a.yml`
- `hls-server/src/main/resources/application-server-b.yml`
- `hls-server/src/main/java/com/rin/hlsserver/config/RandomFailureSimulationFilter.java`
- `hls-server/src/main/java/com/rin/hlsserver/config/RequestLoggingFilter.java`
- `hls-server/src/main/java/com/rin/hlsserver/model/SystemLog.java`
- `hls-server/src/main/java/com/rin/hlsserver/repository/SystemLogRepository.java`
- `hls-server/src/main/java/com/rin/hlsserver/service/SystemLogService.java`
- `hls-server/src/main/resources/db/migration/create_system_logs.sql`
- `hls-server/src/main/resources/db/migration/add_structured_fields_to_system_logs.sql`
- `hls-server/src/main/java/com/rin/hlsserver/monitor/gui/SwingMonitorFrame.java`
- `hls-server/src/main/java/com/rin/hlsserver/monitor/service/MonitorTrackerService.java`
- `run-multi-server-demo.sh`
- `stop-multi-server-demo.sh`
- `RUN.md`