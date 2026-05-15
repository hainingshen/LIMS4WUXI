#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

API_BASE="${API_BASE:-http://localhost:8080/api/v1}"
TMP_DIR="$(mktemp -d)"
LAST_BODY="$TMP_DIR/body.json"

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  if [[ -f "$LAST_BODY" ]]; then
    printf 'Last response body:\n' >&2
    cat "$LAST_BODY" >&2 || true
    printf '\n' >&2
  fi
  exit 1
}

need() {
  command -v "$1" >/dev/null 2>&1 || fail "$1 is required"
}

need curl
need jq

request() {
  local method="$1"
  local path="$2"
  local expected="$3"
  local token="${4:-}"
  local data="${5:-}"
  local status
  local args=(-sS -o "$LAST_BODY" -w "%{http_code}" -X "$method" "$API_BASE$path")

  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$data" ]]; then
    args+=(-H "Content-Type: application/json" --data "$data")
  fi

  status="$(curl "${args[@]}" || true)"
  printf '%s %s -> %s\n' "$method" "$path" "$status" >&2
  [[ "$status" == "$expected" ]] || fail "expected $expected for $method $path, got $status"
}

login() {
  local channel="$1"
  local username="$2"
  local password="$3"
  local payload
  payload="$(jq -nc --arg username "$username" --arg password "$password" \
    '{username:$username,password:$password}')"
  request POST "/auth/$channel/login" 200 "" "$payload"
  jq -er '.accessToken' "$LAST_BODY"
}

internal_token="$(login internal internal.admin Internal123!)"
portal_token="$(login portal portal.applicant Applicant123!)"

for path in \
  /study-plans \
  /work-orders \
  /raw-records \
  /reports \
  /payment-receipts \
  /green-channel-requests \
  /change-requests \
  /sample-retentions \
  /archive-items \
  /device-bookings \
  /inventory-batches \
  /inventory-transactions \
  /customer-orgs \
  /portal-users \
  /system-configs \
  /business-rules \
  /audit-logs \
  /report-templates \
  /projects \
  /quotations \
  /contracts
do
  request GET "$path" 403 "$portal_token"
  jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "$path did not return FORBIDDEN"
done

for path in /projects /quotations /contracts /archive-items /device-bookings /inventory-batches /inventory-transactions /customer-orgs /portal-users
do
  request GET "$path" 200 "$internal_token"
  jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
    || fail "$path did not return a paged response"
done

for path in /system-configs /business-rules
do
  request GET "$path" 200 "$internal_token"
  jq -e 'type == "array"' "$LAST_BODY" >/dev/null || fail "$path did not return an array response"
done

request GET "/audit-logs" 200 "$internal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/audit-logs did not return a paged response"

missing_blocker_id="00000000-0000-0000-0000-00000000ffff"
request POST "/work-center/blockers/$missing_blocker_id/close" 404 "$internal_token" \
  '{"resolution":"workflow gap regression"}'
jq -e '.code == "NOT_FOUND"' "$LAST_BODY" >/dev/null || fail "missing blocker close did not return NOT_FOUND"

printf 'WORKFLOW GAP REGRESSION PASSED\n'
