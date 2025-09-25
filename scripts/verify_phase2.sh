#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
BASE_URL=${BASE_URL:-http://localhost:8080}
ENV_FILE="$ROOT_DIR/infra/.env"

if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC2046
  export $(grep -E '^(DB_|SERVICE_BASE_URL)' "$ENV_FILE" | xargs)
fi

DB_NAME=${DB_NAME:-hp_vvs}
DB_USERNAME=${DB_USERNAME:-hpvvs}
DB_PASSWORD=${DB_PASSWORD:-}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
if command -v jq >/dev/null 2>&1 && command -v psql >/dev/null 2>&1 && command -v curl >/dev/null 2>&1; then
  :
else
  echo "curl, psql, and jq are required for verify_phase2.sh" >&2
  exit 1
fi

if [ -n "$DB_PASSWORD" ]; then
  PSQL_DSN="postgresql://$DB_USERNAME:$DB_PASSWORD@$DB_HOST:$DB_PORT/$DB_NAME"
else
  PSQL_DSN="postgresql://$DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"
fi

TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT

CLIENT_PAYLOAD=$(cat <<JSON
{
  "rootApptId": "HP-1001",
  "salesStage": "CONSULT",
  "conversionStatus": "OPEN",
  "customOrderStatus": "N/A",
  "inProductionStatus": null,
  "centerStoneOrderStatus": null,
  "nextSteps": "Book DV",
  "assistedRep": "Alex",
  "updatedBy": "phase2-cli@local"
}
JSON
)

CLIENT_RESP="$TEMP_DIR/client_status.json"
curl -sf -X POST "$BASE_URL/client-status/submit" \
  -H 'Content-Type: application/json' \
  -d "$CLIENT_PAYLOAD" > "$CLIENT_RESP"

jq -e '.rootApptId == "HP-1001" and .status == "OK"' "$CLIENT_RESP" >/dev/null

CLIENT_DUP_RESP="$TEMP_DIR/client_status_duplicate.json"
curl -sf -X POST "$BASE_URL/client-status/submit" \
  -H 'Content-Type: application/json' \
  -d "$CLIENT_PAYLOAD" > "$CLIENT_DUP_RESP"

jq -e '.status == "UNCHANGED"' "$CLIENT_DUP_RESP" >/dev/null

CLIENT_LOG_COUNT=$(psql "$PSQL_DSN" -Atc "SELECT COUNT(*) FROM client_status_log WHERE root_appt_id = 'HP-1001'")
if [ "$CLIENT_LOG_COUNT" -lt 1 ]; then
  echo "Expected client_status_log row for HP-1001" >&2
  exit 1
fi

CLIENT_ENTRY_COUNT=$(psql "$PSQL_DSN" -Atc "SELECT COUNT(*) FROM per_client_entries WHERE root_appt_id = 'HP-1001' AND deadline_type IS NULL")
if [ "$CLIENT_ENTRY_COUNT" -ne 1 ]; then
  echo "Expected single per_client_entries status row for HP-1001" >&2
  exit 1
fi

CLIENT_REPORT_COUNT=$(psql "$PSQL_DSN" -Atc "SELECT COUNT(*) FROM per_client_reports WHERE root_appt_id = 'HP-1001'")
if [ "$CLIENT_REPORT_COUNT" -ne 1 ]; then
  echo "Expected single per_client_reports row for HP-1001" >&2
  exit 1
fi

ROW14_COUNT=$(psql "$PSQL_DSN" -Atc "SELECT COUNT(*) FROM per_client_entries WHERE root_appt_id = 'HP-1001'")
if [ "$ROW14_COUNT" -lt 1 ]; then
  echo "Expected per_client_entries row for HP-1001" >&2
  exit 1
fi

DEADLINE_PAYLOAD=$(cat <<JSON
{
  "rootApptId": "HP-1001",
  "deadlineType": "3D",
  "deadlineDate": "2025-10-01",
  "movedBy": "phase2-cli@local",
  "assistedRep": "Alex"
}
JSON
)

DEADLINE_RESP="$TEMP_DIR/deadline.json"
curl -sf -X POST "$BASE_URL/deadlines/record" \
  -H 'Content-Type: application/json' \
  -d "$DEADLINE_PAYLOAD" > "$DEADLINE_RESP"

jq -e '.rootApptId == "HP-1001" and .deadlineType == "3D" and .moveCount == 1' "$DEADLINE_RESP" >/dev/null

DEADLINE_DUP_RESP="$TEMP_DIR/deadline_dup.json"
curl -sf -X POST "$BASE_URL/deadlines/record" \
  -H 'Content-Type: application/json' \
  -d "$DEADLINE_PAYLOAD" > "$DEADLINE_DUP_RESP"

jq -e '.moveCount == 1' "$DEADLINE_DUP_RESP" >/dev/null

DEADLINE_CHANGED_PAYLOAD=$(cat <<JSON
{
  "rootApptId": "HP-1001",
  "deadlineType": "3D",
  "deadlineDate": "2025-10-05T12:30:00Z",
  "movedBy": "phase2-cli@local",
  "assistedRep": "Alex"
}
JSON
)

DEADLINE_CHANGED_RESP="$TEMP_DIR/deadline_changed.json"
curl -sf -X POST "$BASE_URL/deadlines/record" \
  -H 'Content-Type: application/json' \
  -d "$DEADLINE_CHANGED_PAYLOAD" > "$DEADLINE_CHANGED_RESP"

jq -e '.moveCount == 2' "$DEADLINE_CHANGED_RESP" >/dev/null

THREED_MOVES=$(psql "$PSQL_DSN" -Atc "SELECT three_d_deadline_moves FROM master WHERE root_appt_id = 'HP-1001'")
if [ "$THREED_MOVES" -ne 2 ]; then
  echo "Expected three_d_deadline_moves == 2 after change, found $THREED_MOVES" >&2
  exit 1
fi

DEADLINE_ENTRY_COUNT=$(psql "$PSQL_DSN" -Atc "SELECT COUNT(*) FROM per_client_entries WHERE root_appt_id = 'HP-1001' AND deadline_type = '3D'")
if [ "$DEADLINE_ENTRY_COUNT" -lt 1 ]; then
  echo "Expected per_client_entries deadline row for HP-1001" >&2
  exit 1
fi

echo "Phase 2 CLI verification completed successfully."
