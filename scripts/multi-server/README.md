# Multi-Server Scripts (Run Le)

Folder nay chua bo script chay rieng le tung phan cho demo A/B + Nginx + Postgres.

## 1. Cac script

- `start-infra.sh`: chay `postgres` + `nginx` bang docker compose
- `stop-infra.sh`: dung `postgres` + `nginx`
- `start-server-a.sh`: chay backend A (`server-a`, port 8081)
- `start-server-b.sh`: chay backend B (`server-b`, port 8082)
- `stop-server-a.sh`: dung backend A
- `stop-server-b.sh`: dung backend B
- `start-all.sh`: chay tat ca (infra + A + B)
- `stop-all.sh`: dung tat ca
- `status.sh`: xem trang thai infra va backend

## 2. Cach chay

Tu root project:

```bash
bash scripts/multi-server/start-infra.sh
bash scripts/multi-server/start-server-a.sh
bash scripts/multi-server/start-server-b.sh
bash scripts/multi-server/status.sh
```

Hoac chay full:

```bash
bash scripts/multi-server/start-all.sh
```

Dung full:

```bash
bash scripts/multi-server/stop-all.sh
```

## 3. Bien moi truong co the override

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `5432`)
- `DB_NAME` (default: `hlsdb`)
- `DB_USER` (default: `root`)
- `DB_PASSWORD` (default: `root`)
- `HLS_STORAGE_PATH` (default: `/tmp/hls-data/videos/hls`)
- `APP_SIMULATION_FAILURE_RATE` (default: `0.2`)

Vi du:

```bash
APP_SIMULATION_FAILURE_RATE=0.1 HLS_STORAGE_PATH=/tmp/hls-demo bash scripts/multi-server/start-server-a.sh
```

## 4. Log va pid

- PID: `.demo-pids/server-a.pid`, `.demo-pids/server-b.pid`
- Log: `.demo-logs/server-a.log`, `.demo-logs/server-b.log`

## 5. Ghi chu

- Script nay khong thay doi logic HLS.
- Nginx va Postgres chay trong Docker, backend A/B chay process Java tren host.
- Neu ban da dung `run-multi-server-demo.sh`/`stop-multi-server-demo.sh` thi van co the dung song song; bo script trong folder nay la cach chay tach roi tung phan.
