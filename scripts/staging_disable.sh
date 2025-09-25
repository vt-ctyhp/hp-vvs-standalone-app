#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"

unset FEATURE_DIAMONDS
unset FEATURE_PAYMENTS

if command -v docker >/dev/null 2>&1; then
  docker compose -f "$COMPOSE_FILE" stop svc-java >/dev/null 2>&1 || true
fi

echo "Staging disabled"
