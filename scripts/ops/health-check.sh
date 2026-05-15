#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080/api/v1}"
trace_id="${LIMS_TRACE_ID:-ops-health-$(date -u +%Y%m%dT%H%M%SZ)}"
tmp_body="$(mktemp)"
trap 'rm -f "$tmp_body"' EXIT

status="$(curl -sS -o "$tmp_body" -w '%{http_code}' \
  -H "X-Trace-Id: $trace_id" \
  "$API_BASE/ops/health" || true)"

cat "$tmp_body"
printf '\n'

if [[ "$status" != "200" ]]; then
  printf 'ERROR: health check returned HTTP %s for trace %s\n' "$status" "$trace_id" >&2
  exit 1
fi

jq -e '.status == "UP" and .checks.database == "UP" and .checks.objectStorage == "UP"' "$tmp_body" >/dev/null
