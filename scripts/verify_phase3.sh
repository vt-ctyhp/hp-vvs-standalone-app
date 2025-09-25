#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
FIXTURES_DIR="$ROOT_DIR/fixtures"
DIAMONDS_FIXTURE="$FIXTURES_DIR/diamonds.sample.csv"
BASE_URL=${BASE_URL:-http://localhost:8080}
ENV_FILE="$ROOT_DIR/infra/.env"
DEFAULT_IFS=$' \t\n'

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: required command '$1' not found" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd jq
require_cmd psql
require_cmd python3

if [ ! -f "$DIAMONDS_FIXTURE" ]; then
  echo "Diamonds fixture not found at $DIAMONDS_FIXTURE" >&2
  exit 1
fi

if [ -f "$ENV_FILE" ]; then
  # shellcheck disable=SC2046
  export $(grep -E '^(DB_|SERVICE_BASE_URL|FEATURE_DIAMONDS)' "$ENV_FILE" | xargs)
fi

DB_NAME=${DB_NAME:-hp_vvs}
DB_USERNAME=${DB_USERNAME:-hpvvs}
DB_PASSWORD=${DB_PASSWORD:-hpvvs}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}

if [ -n "$DB_PASSWORD" ]; then
  export PGPASSWORD="$DB_PASSWORD"
fi
PSQL_DSN="postgresql://$DB_USERNAME@$DB_HOST:$DB_PORT/$DB_NAME"

wait_for_port() {
  local url="$1"
  local name="$2"
  for _ in {1..60}; do
    if curl -sf "$url" >/dev/null; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for $name at $url" >&2
  exit 1
}

wait_for_db() {
  for _ in {1..60}; do
    if PGPASSWORD="$DB_PASSWORD" psql "$PSQL_DSN" -c "SELECT 1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "Timed out waiting for Postgres at $DB_HOST:$DB_PORT" >&2
  exit 1
}

wait_for_db
wait_for_port "$BASE_URL/health" "service health endpoint"

psql "$PSQL_DSN" <<SQL
TRUNCATE TABLE diamonds_summary_100 RESTART IDENTITY;
TRUNCATE TABLE diamonds_orders_200 RESTART IDENTITY;
SQL

psql "$PSQL_DSN" -c "\\copy diamonds_orders_200 (stone_reference, root_appt_id, order_status, stone_status, stone_type, ordered_by, ordered_date, memo_invoice_date, return_due_date, decided_by, decided_date) FROM '$DIAMONDS_FIXTURE' WITH (FORMAT csv, HEADER true, NULL '')"

order_payload=$(cat <<'JSON'
{
  "items": [
    {
      "rootApptId": "HP-1001",
      "decision": "On the Way",
      "orderedBy": "Casey Stone",
      "orderedDate": "2025-02-03"
    },
    {
      "rootApptId": "HP-1003",
      "decision": "Not Approved"
    }
  ],
  "defaultOrderedBy": "Jamie Lee",
  "defaultOrderedDate": "2025-02-01",
  "applyDefaultsToAll": false
}
JSON
)

order_response=$(curl -sf -H 'Content-Type: application/json' -d "$order_payload" "$BASE_URL/diamonds/order-approvals")

echo "$order_response" | jq -e '.results | length == 2' >/dev/null

echo "$order_response" | jq -e '.results[] | select(.rootApptId == "HP-1001") | .centerStoneOrderStatus == "On the Way"' >/dev/null

echo "$order_response" | jq -e '.results[] | select(.rootApptId == "HP-1003") | .centerStoneOrderStatus == "Not Approved"' >/dev/null

hp1001_order=$(psql "$PSQL_DSN" -Atc "SELECT order_status, ordered_by, ordered_date FROM diamonds_orders_200 WHERE stone_reference = 'CST-001'")
IFS='|' read -r status1 ordered_by1 ordered_date1 <<<"$hp1001_order"
IFS="$DEFAULT_IFS"
if [ "$status1" != "On the Way" ] || [ "$ordered_by1" != "Casey Stone" ] || [ "$ordered_date1" != "2025-02-03" ]; then
  echo "Order approvals failed for HP-1001" >&2
  exit 1
fi

hp1003_order=$(psql "$PSQL_DSN" -Atc "SELECT order_status, ordered_by, ordered_date FROM diamonds_orders_200 WHERE stone_reference = 'DV-003'")
IFS='|' read -r status2 ordered_by2 ordered_date2 <<<"$hp1003_order"
IFS="$DEFAULT_IFS"
if [ "$status2" != "Not Approved" ] || [ "$ordered_by2" != "Jamie Lee" ] || [ "$ordered_date2" != "2025-02-01" ]; then
  echo "Order approvals defaults failed for HP-1003" >&2
  exit 1
fi

summary_before=$(psql "$PSQL_DSN" -Atc "SELECT on_the_way_count, total_count, to_char(updated_at, 'YYYY-MM-DD"T"HH24:MI:SSOF') FROM diamonds_summary_100 WHERE root_appt_id = 'HP-1001'")
IFS='|' read -r on_way_before total_before updated_before <<<"$summary_before"
IFS="$DEFAULT_IFS"

order_repeat=$(curl -sf -H 'Content-Type: application/json' -d "$order_payload" "$BASE_URL/diamonds/order-approvals")

echo "$order_repeat" | jq -e '[.results[].affectedRows] | add == 0' >/dev/null

summary_after=$(psql "$PSQL_DSN" -Atc "SELECT on_the_way_count, total_count, to_char(updated_at, 'YYYY-MM-DD"T"HH24:MI:SSOF') FROM diamonds_summary_100 WHERE root_appt_id = 'HP-1001'")
IFS='|' read -r on_way_after total_after updated_after <<<"$summary_after"
IFS="$DEFAULT_IFS"
if [ "$on_way_before" != "$on_way_after" ] || [ "$total_before" != "$total_after" ] || [ "$updated_before" != "$updated_after" ]; then
  echo "Order approvals idempotency failed" >&2
  exit 1
fi

confirm_payload=$(cat <<'JSON'
{
  "items": [
    {
      "rootApptId": "HP-1001",
      "memoDate": "2025-02-05",
      "selected": true
    }
  ],
  "applyDefaultToAll": false
}
JSON
)

confirm_response=$(curl -sf -H 'Content-Type: application/json' -d "$confirm_payload" "$BASE_URL/diamonds/confirm-delivery")

echo "$confirm_response" | jq -e '.results | length == 1' >/dev/null

delivered_row=$(psql "$PSQL_DSN" -Atc "SELECT order_status, stone_status, memo_invoice_date, return_due_date FROM diamonds_orders_200 WHERE stone_reference = 'DV-001'")
IFS='|' read -r delivered_status delivered_stone memo_date return_due <<<"$delivered_row"
IFS="$DEFAULT_IFS"
if [ "$delivered_status" != "Delivered" ] || [ "$delivered_stone" != "In Stock" ]; then
  echo "Confirm delivery did not update status" >&2
  exit 1
fi
python3 - <<PY
from datetime import datetime, timedelta
memo = datetime.strptime("$memo_date", "%Y-%m-%d")
return_due = datetime.strptime("$return_due", "%Y-%m-%d")
if memo + timedelta(days=20) != return_due:
    raise SystemExit("Return due date mismatch")
PY

confirm_repeat=$(curl -sf -H 'Content-Type: application/json' -d "$confirm_payload" "$BASE_URL/diamonds/confirm-delivery")

echo "$confirm_repeat" | jq -e '[.results[].affectedRows] | add == 0' >/dev/null

stone_payload=$(cat <<'JSON'
{
  "items": [
    {
      "rootApptId": "HP-1002",
      "decision": "Keep",
      "decidedBy": "Alex Harper",
      "decidedDate": "2025-02-06"
    },
    {
      "rootApptId": "HP-1004",
      "decision": "Replace"
    }
  ],
  "defaultDecidedBy": "Morgan Lee",
  "defaultDecidedDate": "2025-02-08",
  "applyDefaultsToAll": false
}
JSON
)

stone_response=$(curl -sf -H 'Content-Type: application/json' -d "$stone_payload" "$BASE_URL/diamonds/stone-decisions")

echo "$stone_response" | jq -e '.results | length == 2' >/dev/null

hp1002_row=$(psql "$PSQL_DSN" -Atc "SELECT stone_status, decided_by, decided_date FROM diamonds_orders_200 WHERE stone_reference = 'CST-002'")
IFS='|' read -r hp1002_status hp1002_decider hp1002_date <<<"$hp1002_row"
IFS="$DEFAULT_IFS"
if [ "$hp1002_status" != "Keep" ] || [ "$hp1002_decider" != "Alex Harper" ] || [ "$hp1002_date" != "2025-02-06" ]; then
  echo "Stone decision keep scenario failed" >&2
  exit 1
fi

hp1004_row=$(psql "$PSQL_DSN" -Atc "SELECT stone_status, decided_by, decided_date FROM diamonds_orders_200 WHERE stone_reference = 'CST-004'")
IFS='|' read -r hp1004_status hp1004_decider hp1004_date <<<"$hp1004_row"
IFS="$DEFAULT_IFS"
if [ "$hp1004_status" != "Replace" ] || [ "$hp1004_decider" != "Morgan Lee" ] || [ "$hp1004_date" != "2025-02-08" ]; then
  echo "Stone decision default application failed" >&2
  exit 1
fi

stone_repeat=$(curl -sf -H 'Content-Type: application/json' -d "$stone_payload" "$BASE_URL/diamonds/stone-decisions")

hp1002_row_repeat=$(psql "$PSQL_DSN" -Atc "SELECT stone_status, decided_by, decided_date FROM diamonds_orders_200 WHERE stone_reference = 'CST-002'")
IFS='|' read -r hp1002_status_repeat hp1002_decider_repeat hp1002_date_repeat <<<"$hp1002_row_repeat"
IFS="$DEFAULT_IFS"
if [ "$hp1002_status_repeat" != "$hp1002_status" ] || [ "$hp1002_decider_repeat" != "$hp1002_decider" ] || [ "$hp1002_date_repeat" != "$hp1002_date" ]; then
  echo "Stone decision idempotency failed for HP-1002" >&2
  exit 1
fi

hp1004_row_repeat=$(psql "$PSQL_DSN" -Atc "SELECT stone_status, decided_by, decided_date FROM diamonds_orders_200 WHERE stone_reference = 'CST-004'")
IFS='|' read -r hp1004_status_repeat hp1004_decider_repeat hp1004_date_repeat <<<"$hp1004_row_repeat"
IFS="$DEFAULT_IFS"
if [ "$hp1004_status_repeat" != "$hp1004_status" ] || [ "$hp1004_decider_repeat" != "$hp1004_decider" ] || [ "$hp1004_date_repeat" != "$hp1004_date" ]; then
  echo "Stone decision idempotency failed for HP-1004" >&2
  exit 1
fi

echo "Phase 3 CLI verification completed successfully."
