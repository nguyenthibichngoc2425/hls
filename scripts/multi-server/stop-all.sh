#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "$SCRIPT_DIR/stop-server-a.sh"
bash "$SCRIPT_DIR/stop-server-b.sh"
bash "$SCRIPT_DIR/stop-infra.sh"

echo "[DEMO] All components are stopped."
