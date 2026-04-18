#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$ROOT_DIR/.demo-pids"

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

stop_backend "server-a"
stop_backend "server-b"

cd "$ROOT_DIR"
docker compose stop nginx postgres >/dev/null
echo "[DEMO] Stopped nginx and postgres containers."