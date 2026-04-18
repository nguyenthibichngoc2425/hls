#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$ROOT_DIR"
echo "[DEMO] Stopping nginx + postgres..."
docker compose stop nginx postgres >/dev/null
echo "[DEMO] Infra stopped."
