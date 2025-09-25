#!/usr/bin/env bash
set -euo pipefail

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required for Phase 4 verification" >&2
  exit 1
fi

SERVICE_BASE_URL="${SERVICE_BASE_URL:-http://localhost:8080}"
ANCHOR_SO="PHASE4-SO-${RANDOM}${RANDOM}"
ROOT_ID="PHASE4-ROOT-${RANDOM}${RANDOM}"
INVOICE_DATE="2024-07-01T17:00:00Z"
RECEIPT_DATE="2024-07-02T19:30:00Z"

post_request() {
  local payload="$1"
  local response
  response=$(curl -s -w '\n%{http_code}' -H 'Content-Type: application/json' -X POST "${SERVICE_BASE_URL}/payments/record" -d "${payload}")
  local http_code
  http_code=$(printf '%s' "${response}" | tail -n1)
  local body
  body=$(printf '%s' "${response}" | sed '$d')
  if [[ "${http_code}" != "200" ]]; then
    echo "Expected HTTP 200 but received ${http_code}: ${body}" >&2
    exit 1
  fi
  printf '%s' "${body}"
}

summary_request() {
  local query="$1"
  local response
  response=$(curl -s -w '\n%{http_code}' "${SERVICE_BASE_URL}/payments/summary?${query}")
  local http_code
  http_code=$(printf '%s' "${response}" | tail -n1)
  local body
  body=$(printf '%s' "${response}" | sed '$d')
  if [[ "${http_code}" != "200" ]]; then
    echo "Expected HTTP 200 but received ${http_code}: ${body}" >&2
    exit 1
  fi
  printf '%s' "${body}"
}

invoice_payload=$(cat <<JSON
{
  "anchorType":"SO",
  "rootApptId":"${ROOT_ID}",
  "soNumber":"${ANCHOR_SO}",
  "docType":"Sales Invoice",
  "paymentDateTime":"${INVOICE_DATE}",
  "method":"Wire",
  "amountGross":500.00,
  "lines":[{"desc":"Custom Ring","qty":1,"amt":500.00}]
}
JSON
)

receipt_payload=$(cat <<JSON
{
  "anchorType":"SO",
  "rootApptId":"${ROOT_ID}",
  "soNumber":"${ANCHOR_SO}",
  "docType":"Deposit Receipt",
  "paymentDateTime":"${RECEIPT_DATE}",
  "method":"Card",
  "amountGross":200.00,
  "feePercent":2.50,
  "reference":"AUTH-${ANCHOR_SO}",
  "lines":[{"desc":"Deposit","qty":1,"amt":200.00}]
}
JSON
)

invoice_response=$(post_request "${invoice_payload}")
receipt_response=$(post_request "${receipt_payload}")

jq -e '.docRole == "RECEIPT" and (.amountNet | tonumber) > 0 and (.subtotal | tonumber) == 200.00' <<<"${receipt_response}" >/dev/null

summary_body=$(summary_request "soNumber=${ANCHOR_SO}")

jq -e '((.invoicesLinesSubtotal - 500) | fabs) < 0.01' <<<"${summary_body}" >/dev/null
jq -e '((.totalPayments - 195) | fabs) < 0.01' <<<"${summary_body}" >/dev/null
jq -e '((.netLinesMinusPayments - 305) | fabs) < 0.01' <<<"${summary_body}" >/dev/null
jq -e '.byMethod.Card and (.byMethod.Card | tonumber | . > 0)' <<<"${summary_body}" >/dev/null
jq -e '(.entries | length) == 2' <<<"${summary_body}" >/dev/null

summary_root=$(summary_request "rootApptId=${ROOT_ID}")
if ! diff <(jq -S '.' <<<"${summary_body}") <(jq -S '.' <<<"${summary_root}") >/dev/null; then
  echo "Root summary did not match SO summary" >&2
  exit 1
fi

repost_response=$(post_request "${receipt_payload}")
if [[ $(jq -r '.status' <<<"${repost_response}") != "UPDATED" ]]; then
  echo "Reposting did not return UPDATED status" >&2
  exit 1
fi

summary_after=$(summary_request "soNumber=${ANCHOR_SO}")
if ! diff <(jq -S '.' <<<"${summary_body}") <(jq -S '.' <<<"${summary_after}") >/dev/null; then
  echo "Summary changed after idempotent re-post" >&2
  exit 1
fi

echo "Phase 4 CLI verification completed successfully."
