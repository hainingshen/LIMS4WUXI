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
finance_token="$(login internal internal.finance Sales123!)"
sample_token="$(login internal internal.sample Sales123!)"
portal_token="$(login portal portal.applicant Applicant123!)"
portal_viewer_token="$(login portal portal.viewer Viewer123!)"

request GET "/projects" 401
jq -e '.code == "UNAUTHORIZED"' "$LAST_BODY" >/dev/null || fail "missing token did not return UNAUTHORIZED"

request GET "/auth/me" 200 "$portal_token"
portal_org_id="$(jq -er '.customerOrgId' "$LAST_BODY")"

for path in /submissions /projects /reports /contracts
do
  request GET "$path?page=1&pageSize=100" 200 "$portal_viewer_token"
  jq -e '.total == 0 and (.items | length == 0)' "$LAST_BODY" >/dev/null \
    || fail "portal viewer without grants should not see $path"
done

for path in \
  /study-plans \
  /work-orders \
  /raw-records \
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
  /holidays \
  /organization-units \
  /users \
  /roles \
  /legacy-status-mappings \
  /report-templates \
  /quotations
do
  request GET "$path" 403 "$portal_token"
  jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "$path did not return FORBIDDEN"
done

request POST "/payment-receipts" 403 "$portal_token" '{}'
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null \
  || fail "unauthorized payment create should be blocked before body validation"

request POST "/payment-receipts" 400 "$finance_token" '{}'
jq -e '.code == "VALIDATION_FAILED"' "$LAST_BODY" >/dev/null \
  || fail "authorized payment create with invalid body should return validation error"

request POST "/users" 403 "$portal_token" '{}'
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null \
  || fail "unauthorized internal user create should be blocked before body validation"

request POST "/users" 400 "$internal_token" '{}'
jq -e '.code == "VALIDATION_FAILED"' "$LAST_BODY" >/dev/null \
  || fail "authorized internal user create with invalid body should return validation error"

fake_id="00000000-0000-0000-0000-00000000ffff"
request POST "/samples/$fake_id/receive" 403 "$portal_token" '{}'
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null \
  || fail "unauthorized sample receive should be blocked before body validation"

sample_receive_payload="$(jq -nc '{
  receivedAt: "2026-05-15T00:10:00Z",
  receivedBy: "00000000-0000-0000-0000-000000000105",
  checkPassed: true
}')"
request POST "/samples/$fake_id/receive" 404 "$sample_token" "$sample_receive_payload"
jq -e '.code == "NOT_FOUND"' "$LAST_BODY" >/dev/null \
  || fail "authorized sample receive with missing id should return NOT_FOUND"

for path in /dictionaries /roles
do
  request POST "$path" 405 "$internal_token" '{}'
  jq -e '.code == "METHOD_NOT_ALLOWED"' "$LAST_BODY" >/dev/null \
    || fail "$path POST did not return METHOD_NOT_ALLOWED"
done

request GET "/projects?page=1&pageSize=100" 200 "$portal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/projects did not return a portal-scoped paged response"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "/projects returned data outside portal customer org $portal_org_id"
cp "$LAST_BODY" "$TMP_DIR/portal-projects.json"

request GET "/submissions?page=1&pageSize=100" 200 "$portal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/submissions did not return a portal-scoped paged response"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "/submissions returned data outside portal customer org $portal_org_id"

request GET "/reports?page=1&pageSize=100" 200 "$portal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/reports did not return a portal-scoped paged response"
jq -e --slurpfile projects "$TMP_DIR/portal-projects.json" \
  '($projects[0].items | map(.id)) as $ids | all(.items[]?; .projectId as $project_id | ($ids | index($project_id) != null))' \
  "$LAST_BODY" >/dev/null || fail "/reports returned data outside visible portal projects"

request GET "/reports?page=1&pageSize=100" 200 "$internal_token"
cross_report_id="$(jq -r --slurpfile projects "$TMP_DIR/portal-projects.json" '
  ($projects[0].items | map(.id)) as $ids
  | [.items[]? | .projectId as $project_id | select(($ids | index($project_id)) == null) | .id][0] // empty
' "$LAST_BODY")"
if [[ -n "$cross_report_id" ]]; then
  request GET "/reports/$cross_report_id/official-pdf" 403 "$portal_token"
  jq -e '.code == "FORBIDDEN" and .details.actionCode == "REPORT_OFFICIAL_DOWNLOAD"' "$LAST_BODY" >/dev/null \
    || fail "cross-customer official PDF did not return REPORT_OFFICIAL_DOWNLOAD forbidden"
fi

request GET "/contracts?page=1&pageSize=100" 200 "$portal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/contracts did not return a portal-scoped paged response"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "/contracts returned data outside portal customer org $portal_org_id"

for path in /projects /quotations /contracts /archive-items /device-bookings /inventory-batches /inventory-transactions /customer-orgs /portal-users
do
  request GET "$path" 200 "$internal_token"
  jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
    || fail "$path did not return a paged response"
done

for path in /system-configs /business-rules /holidays /organization-units /roles /legacy-status-mappings
do
  request GET "$path" 200 "$internal_token"
  jq -e 'type == "array"' "$LAST_BODY" >/dev/null || fail "$path did not return an array response"
done

request GET "/users" 200 "$internal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/users did not return a paged response"

request GET "/audit-logs" 200 "$internal_token"
jq -e '.items and (.page >= 1) and (.pageSize >= 1) and (.total >= 0)' "$LAST_BODY" >/dev/null \
  || fail "/audit-logs did not return a paged response"

missing_blocker_id="00000000-0000-0000-0000-00000000ffff"
request POST "/work-center/blockers/$missing_blocker_id/close" 404 "$internal_token" \
  '{"resolution":"workflow gap regression"}'
jq -e '.code == "NOT_FOUND"' "$LAST_BODY" >/dev/null || fail "missing blocker close did not return NOT_FOUND"

printf 'WORKFLOW GAP REGRESSION PASSED\n'
