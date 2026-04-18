#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/common.sh"

print_backend_status() {
    local server_name="$1"
    local pid_file="$PID_DIR/${server_name}.pid"

    if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
        echo "[DEMO] ${server_name}: RUNNING (pid $(cat "$pid_file"))"
    else
        echo "[DEMO] ${server_name}: STOPPED"
    fi
}

print_backend_status "server-a"
print_backend_status "server-b"

cd "$ROOT_DIR"
if docker compose ps --status running | grep -q "hls-postgres"; then
    echo "[DEMO] postgres: RUNNING"
else
    echo "[DEMO] postgres: STOPPED"
fi

if docker compose ps --status running | grep -q "hls-lb"; then
    echo "[DEMO] nginx: RUNNING"
else
    echo "[DEMO] nginx: STOPPED"
fi
