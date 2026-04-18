#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "$ROOT_DIR"
echo "[DEMO] Starting postgres + nginx..."
docker compose up -d postgres nginx
echo "[DEMO] Infra is up."
