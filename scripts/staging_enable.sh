#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
COMPOSE_FILE="$ROOT_DIR/infra/docker-compose.yml"
BASE_URL=${BASE_URL:-http://localhost:8080}
SVC_DIR="$ROOT_DIR/svc-java"
FEATURE_DIAMONDS="true"
FEATURE_PAYMENTS="true"
FEATURE_REPORTS=${FEATURE_REPORTS:-false}
DB_USERNAME=${DB_USERNAME:-hpvvs}
DB_PASSWORD=${DB_PASSWORD:-hpvvs}
DB_NAME=${DB_NAME:-hp_vvs}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
export FEATURE_DIAMONDS FEATURE_PAYMENTS FEATURE_REPORTS BASE_URL DB_USERNAME DB_PASSWORD DB_NAME DB_HOST DB_PORT

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: required command '$1' not found" >&2
    exit 1
  fi
}

require_cmd docker
if ! docker compose version >/dev/null 2>&1; then
  echo "Error: docker compose plugin is required" >&2
  exit 1
fi
require_cmd curl
require_cmd jq
require_cmd psql

wait_for_health() {
  local url="$1"
  for _ in {1..60}; do
    if curl -sf "$url" >/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for service health at $url" >&2
  exit 1
}

start_services_if_defined() {
  local services
  services=$(docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null || true)
  if echo "$services" | grep -qx "postgres"; then
    docker compose -f "$COMPOSE_FILE" up -d postgres
  fi
  if echo "$services" | grep -qx "svc-java"; then
    docker compose -f "$COMPOSE_FILE" up -d svc-java
    return 0
  fi
  return 1
}

BOOTRUN_PID=""
cleanup() {
  if [ -n "$BOOTRUN_PID" ] && ps -p "$BOOTRUN_PID" >/dev/null 2>&1; then
    kill "$BOOTRUN_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if ! start_services_if_defined; then
  echo "svc-java service not found in docker compose; starting via gradle bootRun" >&2
  if docker compose -f "$COMPOSE_FILE" config --services 2>/dev/null | grep -qx "postgres"; then
    docker compose -f "$COMPOSE_FILE" up -d postgres
  fi
  (cd "$SVC_DIR" && FEATURE_DIAMONDS=true FEATURE_PAYMENTS=true FEATURE_REPORTS="$FEATURE_REPORTS" ./gradlew bootRun >/tmp/staging-boot.log 2>&1) &
  BOOTRUN_PID=$!
  sleep 5
fi

wait_for_health "$BASE_URL/health"

bash "$ROOT_DIR/scripts/seed.sh"

bash "$ROOT_DIR/scripts/verify_phase3.sh"
bash "$ROOT_DIR/scripts/verify_phase4.sh"

if [[ "${FEATURE_REPORTS}" == "true" ]]; then
  bash "$ROOT_DIR/scripts/verify_phase5.sh"
fi

echo "Staging verification completed successfully."
