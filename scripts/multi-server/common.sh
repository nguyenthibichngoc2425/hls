#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PID_DIR="$ROOT_DIR/.demo-pids"
LOG_DIR="$ROOT_DIR/.demo-logs"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-hlsdb}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
JVM_TIMEZONE="${JVM_TIMEZONE:-Asia/Ho_Chi_Minh}"

HLS_STORAGE_PATH="${HLS_STORAGE_PATH:-/tmp/hls-data/videos/hls}"
APP_SIMULATION_FAILURE_RATE="${APP_SIMULATION_FAILURE_RATE:-0.0}"
APP_VIEWER_MAX_ACTIVE_PER_SERVER="${APP_VIEWER_MAX_ACTIVE_PER_SERVER:-2}"
APP_VIEWER_TTL_SECONDS="${APP_VIEWER_TTL_SECONDS:-15}"
APP_VIEWER_CLEANUP_INTERVAL_MS="${APP_VIEWER_CLEANUP_INTERVAL_MS:-5000}"

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
        APP_VIEWER_MAX_ACTIVE_PER_SERVER="$APP_VIEWER_MAX_ACTIVE_PER_SERVER" \
        APP_VIEWER_TTL_SECONDS="$APP_VIEWER_TTL_SECONDS" \
        APP_VIEWER_CLEANUP_INTERVAL_MS="$APP_VIEWER_CLEANUP_INTERVAL_MS" \
                bash ./mvnw -q spring-boot:run \
                    -Dspring-boot.run.profiles="$profile" \
                    -Dspring-boot.run.jvmArguments="-Duser.timezone=${JVM_TIMEZONE}" >"$log_file" 2>&1 &
        echo $! >"$pid_file"
    )

    echo "[DEMO] Started ${server_name} on :${port} (log: ${log_file})"
}

stop_backend() {
    local server_name="$1"
    local pid_file="$PID_DIR/${server_name}.pid"

    if [[ ! -f "$pid_file" ]]; then
        echo "[DEMO] ${server_name} is not running (no pid file)."
        return
    fi

    local pid
    pid="$(cat "$pid_file")"
    if kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
        echo "[DEMO] Stopped ${server_name} (pid ${pid})."
    else
        echo "[DEMO] ${server_name} pid ${pid} was already stopped."
    fi

    rm -f "$pid_file"
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
