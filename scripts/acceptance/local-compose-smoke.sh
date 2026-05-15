#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/local/docker-env.sh
. "$ROOT_DIR/scripts/local/docker-env.sh"

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
select_docker_cmd || fail "docker daemon is not reachable"

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

submission_payload() {
  local suffix="$1"
  jq -nc --arg suffix "$suffix" '{
    formCode: "SUBMISSION_VER15",
    schemaVersion: 1,
    data: {
      sponsorNameZh: ("Local Smoke Sponsor " + $suffix),
      sponsorNameEn: ("Local Smoke Sponsor " + $suffix),
      sponsorAddressZh: "No. 23 Local Smoke Road",
      sponsorAddressEn: "No. 23 Local Smoke Road",
      manufacturerNameZh: "Local Smoke Manufacturer",
      manufacturerNameEn: "Local Smoke Manufacturer",
      manufacturerAddressZh: "No. 23 Manufacturer Road",
      manufacturerAddressEn: "No. 23 Manufacturer Road",
      contactName: "Local Smoke Contact",
      contactPhone: "13800000023",
      contactEmail: ("local-smoke-" + $suffix + "@example.com"),
      officialReportEmail: ("local-smoke-report-" + $suffix + "@example.com"),
      shippingInfo: "Ship to local smoke sponsor",
      requiresGlp: false,
      requiresHardCopyOfElectronicReport: false,
      protocolLanguage: "CHS",
      reportLanguage: "CHS",
      submittedQuantityText: "10 pcs",
      sampleDisposition: "DESTROY_DISCARD",
      sampleNameZh: ("Local Smoke Sample " + $suffix),
      sampleNameEn: ("Local Smoke Sample " + $suffix),
      sterilizationState: "NON_STERILE",
      casCode: "N/A",
      modelRef: "MODEL-LOCAL",
      sizeSpec: "10 mm",
      lotBatch: ("LOT-" + $suffix),
      rawMaterial: "Polymer",
      packagingMaterial: "Pouch",
      physicalState: "SOLID",
      color: "White",
      density: "N/A",
      stability: "Stable",
      solubility: "Insoluble",
      storageCondition: "ROOM_TEMPERATURE",
      intendedUse: "Local deployment smoke validation",
      additionalInfo: "No additional info",
      canBeCut: true,
      componentsToTestZh: "N/A",
      componentsToTestEn: "N/A",
      workingTemperatureCelsius: "37",
      withstand50C: false,
      clinicalContactDuration: "A_LIMITED",
      extractionBasis: "SURFACE_AREA",
      thinnestThicknessMm: "1",
      specialInstructions: "N/A"
    }
  }'
}

db_scalar() {
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a

  "${DOCKER_CMD[@]}" compose exec -T postgres psql \
    -U "${POSTGRES_USER:-lims}" \
    -d "${POSTGRES_DB:-lims}" \
    -Atc "$1"
}

request GET "/auth/me" 401

portal_token="$(login portal portal.applicant Applicant123!)"
internal_token="$(login internal internal.sales Sales123!)"
tech_token="$(login internal internal.tech Sales123!)"
business_token="$(login internal internal.business Sales123!)"

request GET "/auth/me" 200 "$portal_token"
jq -e '.clientType == "PORTAL"' "$LAST_BODY" >/dev/null || fail "portal auth/me did not return PORTAL clientType"

request GET "/auth/me" 200 "$internal_token"
jq -e '.clientType == "INTERNAL"' "$LAST_BODY" >/dev/null || fail "internal auth/me did not return INTERNAL clientType"

request GET "/dictionaries?dictCode=YES_NO" 200 "$portal_token"
jq -e 'length > 0' "$LAST_BODY" >/dev/null || fail "YES_NO dictionary is empty"

request GET "/form-schemas?formCode=SUBMISSION_VER15&status=PUBLISHED" 200 "$portal_token"
jq -e '.[0].formCode == "SUBMISSION_VER15"' "$LAST_BODY" >/dev/null || fail "SUBMISSION_VER15 schema not found"

suffix="$(date +%s)"
payload="$(submission_payload "$suffix")"
request POST "/submissions" 201 "$portal_token" "$payload"
submission_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "DRAFT"' "$LAST_BODY" >/dev/null || fail "created submission is not DRAFT"

request POST "/submissions/$submission_id/submit" 200 "$portal_token"
jq -e '.status == "SUBMITTED"' "$LAST_BODY" >/dev/null || fail "submitted submission is not SUBMITTED"

request GET "/submissions?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$submission_id" '.items | map(.id) | index($id) != null' "$LAST_BODY" >/dev/null \
  || fail "internal submission list does not contain $submission_id"

request POST "/submissions/$submission_id/accept" 403 "$portal_token"
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "portal accept did not return FORBIDDEN"

request POST "/submissions/$submission_id/accept" 200 "$internal_token"
jq -e '.status == "ACCEPTED"' "$LAST_BODY" >/dev/null || fail "sales accept did not return ACCEPTED"

split_payload="$(jq -nc '{
  splitStrategy: "MANUAL",
  projectDrafts: [
    {
      projectType: "STD_ACCREDITED",
      qualificationType: "CMA",
      serviceType: "NORMAL",
      requiresAnimalTest: false
    }
  ]
}')"

request POST "/submissions/$submission_id/split-projects" 403 "$portal_token" "$split_payload"
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "portal split did not return FORBIDDEN"

request POST "/submissions/$submission_id/split-projects" 201 "$internal_token" "$split_payload"
project_id="$(jq -er '.[0].id' "$LAST_BODY")"
jq -e --arg submission_id "$submission_id" \
  '.[0].submissionId == $submission_id and .[0].status == "ACCEPTED"' "$LAST_BODY" >/dev/null \
  || fail "sales split did not return accepted project for submission"

request GET "/projects?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$project_id" '.items | map(.id) | index($id) != null' "$LAST_BODY" >/dev/null \
  || fail "internal project list does not contain $project_id"

nonstandard_split_payload="$(jq -nc '{
  splitStrategy: "MANUAL",
  projectDrafts: [
    {
      projectType: "NON_STANDARD",
      qualificationType: "NONE",
      serviceType: "NORMAL",
      requiresAnimalTest: false
    }
  ]
}')"

request POST "/submissions" 201 "$portal_token" "$payload"
nonstandard_submission_id="$(jq -er '.id' "$LAST_BODY")"
request POST "/submissions/$nonstandard_submission_id/submit" 200 "$portal_token"
request POST "/submissions/$nonstandard_submission_id/accept" 200 "$internal_token"
request POST "/submissions/$nonstandard_submission_id/split-projects" 201 "$internal_token" "$nonstandard_split_payload"
nonstandard_project_id="$(jq -er '.[0].id' "$LAST_BODY")"
jq -e '.[0].projectType == "NON_STANDARD" and .[0].status == "ACCEPTED"' "$LAST_BODY" >/dev/null \
  || fail "nonstandard split did not return accepted nonstandard project"

quotation_payload="$(jq -nc '{
  items: [
    {
      testName: "Cytotoxicity",
      unitPrice: 100,
      quantity: 2
    }
  ],
  discountAmount: 10,
  note: "Local smoke quotation"
}')"
request POST "/projects/$nonstandard_project_id/quotations" 409 "$internal_token" "$quotation_payload"
jq -e '.code == "PROJECT_NOT_QUOTABLE"' "$LAST_BODY" >/dev/null || fail "quotation guard did not reject before review"

request POST "/projects/$nonstandard_project_id/nonstandard-review/start" 200 "$internal_token"
jq -e '.status == "NONSTANDARD_REVIEW"' "$LAST_BODY" >/dev/null || fail "nonstandard review did not start"

tech_approval_payload="$(jq -nc '{stage:"TECH",outcome:"APPROVED",comment:"技术通过"}')"
request POST "/projects/$nonstandard_project_id/nonstandard-review/approve" 200 "$tech_token" "$tech_approval_payload"
jq -e '.status == "NONSTANDARD_REVIEW"' "$LAST_BODY" >/dev/null || fail "tech approval should keep review pending"

business_approval_payload="$(jq -nc '{stage:"BUSINESS",outcome:"APPROVED",comment:"业务通过"}')"
request POST "/projects/$nonstandard_project_id/nonstandard-review/approve" 200 "$business_token" "$business_approval_payload"
jq -e '.status == "QUOTABLE"' "$LAST_BODY" >/dev/null || fail "business approval did not make project quotable"

request POST "/projects/$nonstandard_project_id/quotations" 201 "$internal_token" "$quotation_payload"
quotation_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$nonstandard_project_id" \
  '.projectId == $project_id and .totalAmount == 190' "$LAST_BODY" >/dev/null \
  || fail "quotation creation did not return expected project and amount"

audit_count="$(db_scalar "select count(*) from audit_log where entity_id = '$submission_id' and action_code = 'SUBMISSION_CREATE' and result = 'SUCCESS';")"
[[ "$audit_count" -ge 1 ]] || fail "expected at least one successful SUBMISSION_CREATE audit row, got $audit_count"
printf 'OK: audit_log has %s successful SUBMISSION_CREATE rows for %s\n' "$audit_count" "$submission_id"

printf 'LOCAL COMPOSE SMOKE PASSED: submission %s project %s quotation %s\n' "$submission_id" "$project_id" "$quotation_id"
