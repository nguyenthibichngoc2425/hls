#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "$SCRIPT_DIR/start-infra.sh"
bash "$SCRIPT_DIR/start-server-a.sh"
bash "$SCRIPT_DIR/start-server-b.sh"

echo "[DEMO] Multi-server mode is running (infra + server-a + server-b)."
