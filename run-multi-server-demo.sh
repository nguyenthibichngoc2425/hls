#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT_DIR/.demo-pids"
LOG_DIR="$ROOT_DIR/.demo-logs"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-hlsdb}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"

HLS_STORAGE_PATH="${HLS_STORAGE_PATH:-/tmp/hls-data/videos/hls}"
APP_SIMULATION_FAILURE_RATE="${APP_SIMULATION_FAILURE_RATE:-0.2}"

mkdir -p "$PID_DIR" "$LOG_DIR" "$HLS_STORAGE_PATH"

start_backend() {
    local profile="$1"
    local server_name="$2"
    local port="$3"
    local pid_file="$PID_DIR/${server_name}.pid"
    local log_file="$LOG_DIR/${server_name}.log"

    if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        echo "[DEMO] ${server_name} is already running (pid $(cat "$pid_file"))."
        return
    fi

    (
        cd "$ROOT_DIR/hls-server"
        SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}" \
        SPRING_DATASOURCE_USERNAME="$DB_USER" \
        SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
        APP_HLS_STORAGE_PATH="$HLS_STORAGE_PATH" \
        APP_SIMULATION_FAILURE_RATE="$APP_SIMULATION_FAILURE_RATE" \
        bash ./mvnw -q spring-boot:run -Dspring-boot.run.profiles="$profile" >"$log_file" 2>&1 &
        echo $! >"$pid_file"
    )

    echo "[DEMO] Started ${server_name} on :${port} (log: ${log_file})"
}

wait_for_backend() {
    local server_name="$1"
    local port="$2"

    for _ in $(seq 1 90); do
        local code
        code="$(curl -s -o /dev/null -w "%{http_code}" "http://127.0.0.1:${port}/api/movies" || true)"
        if [[ "$code" != "000" ]]; then
            echo "[DEMO] ${server_name} is ready on :${port} (status ${code})."
            return
        fi
        sleep 1
    done

    echo "[DEMO] WARNING: ${server_name} is not ready after timeout. Check logs in ${LOG_DIR}."
}

echo "[DEMO] Starting PostgreSQL + Nginx load balancer via docker compose..."
cd "$ROOT_DIR"
docker compose up -d postgres nginx

echo "[DEMO] Starting Spring Boot server A and B (non-docker)..."
start_backend "server-a" "server-a" "8081"
start_backend "server-b" "server-b" "8082"

wait_for_backend "server-a" "8081"
wait_for_backend "server-b" "8082"

echo "[DEMO] Done."
echo "[DEMO] Load balancer URL: http://localhost:8080"
echo "[DEMO] Backend A URL: http://localhost:8081"
echo "[DEMO] Backend B URL: http://localhost:8082"
echo "[DEMO] Failure simulation rate: ${APP_SIMULATION_FAILURE_RATE}"
echo "[DEMO] Shared HLS storage path: ${HLS_STORAGE_PATH}"
echo "[DEMO] To stop everything: bash stop-multi-server-demo.sh"