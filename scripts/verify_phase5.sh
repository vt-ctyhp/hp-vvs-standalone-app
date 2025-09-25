#!/usr/bin/env bash
set -euo pipefail

SERVICE_BASE_URL=${SERVICE_BASE_URL:-http://localhost:8080}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: required command '$1' not found" >&2
    exit 1
  fi
}

require_cmd curl
require_cmd jq

request() {
  local path="$1"
  local url="${SERVICE_BASE_URL}${path}"
  local response
  response=$(curl -s -w '\n%{http_code}' "$url") || {
    echo "Request to $url failed" >&2
    exit 1
  }
  local http_code body
  http_code=$(printf '%s' "${response}" | tail -n1)
  body=$(printf '%s' "${response}" | sed '$d')
  if [[ "$http_code" != "200" ]]; then
    echo "Unexpected status ${http_code} from $url: ${body}" >&2
    exit 1
  fi
  printf '%s' "$body"
}

status_body=$(request "/reports/by-status?filters=status:Deposit,includeProductionCols:true")
jq -e '.rows | length > 0' <<<"${status_body}" >/dev/null
jq -e '.rows[0]["Sales Stage"] == "Deposit"' <<<"${status_body}" >/dev/null
jq -e '.rows[0] | has("In Production Status") and has("Production Deadline")' <<<"${status_body}" >/dev/null

rep_body=$(request "/reports/by-rep?filters=brand:hpusa")
jq -e '.rows | length > 0' <<<"${rep_body}" >/dev/null
jq -e 'all(.rows[]; .["Assigned Rep"] != null and .["Assigned Rep"] != "")' <<<"${rep_body}" >/dev/null

kpi_body=$(request "/dashboard/kpis?dateFrom=2024-01-01&dateTo=2024-12-31")
for key in weightedPipeline totalDeposits firstTimeDepositCount overdueProductionCount overdueThreeDCount; do
  jq -e "has(\"${key}\")" <<<"${kpi_body}" >/dev/null
  jq -e ".${key} | (type == \"number\")" <<<"${kpi_body}" >/dev/null
done

printf 'Phase 5 CLI verification completed successfully.\n'
