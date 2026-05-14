#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "$SCRIPT_DIR/start-infra.sh"
source "$SCRIPT_DIR/common.sh"

start_backend "server-a" "server-a" "8081"
wait_for_backend "server-a" "8081"

start_backend "server-b" "server-b" "8082"
wait_for_backend "server-b" "8082"

echo "[DEMO] Multi-server mode is running (infra + server-a + server-b)."
