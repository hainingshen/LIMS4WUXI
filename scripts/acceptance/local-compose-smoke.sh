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
sample_token="$(login internal internal.sample Sales123!)"
sd_token="$(login internal internal.sd Sales123!)"
inspector_token="$(login internal internal.inspector Sales123!)"
qc_token="$(login internal internal.qc Sales123!)"

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

contract_payload="$(jq -nc --arg project_id "$nonstandard_project_id" '{
  projectId: $project_id,
  contractType: "TEST_AGREEMENT",
  templateCode: "TEST_AGREEMENT_VER1"
}')"
request POST "/contracts" 201 "$internal_token" "$contract_payload"
contract_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$nonstandard_project_id" \
  '.projectId == $project_id and .status == "PENDING_SIGN"' "$LAST_BODY" >/dev/null \
  || fail "contract creation did not return pending sign contract"

request GET "/contracts?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$contract_id" '.items | map(.id) | index($id) != null' "$LAST_BODY" >/dev/null \
  || fail "internal contract list does not contain $contract_id"

signed_payload="$(jq -nc '{signedAt:"2026-05-15T00:00:00Z"}')"
request POST "/contracts/$contract_id/mark-signed" 200 "$internal_token" "$signed_payload"
jq -e '.status == "SIGNED" and .signedAt == "2026-05-15T00:00:00Z"' "$LAST_BODY" >/dev/null \
  || fail "contract signing did not return signed contract"

request GET "/projects?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "CONTRACTED")' "$LAST_BODY" >/dev/null \
  || fail "signed contract did not move project to CONTRACTED"

request GET "/samples?keyword=LOT-$suffix&page=1&pageSize=50" 200 "$sample_token"
sample_id="$(jq -er --arg project_id "$nonstandard_project_id" \
  '.items[] | select(.projectId == $project_id) | .id' "$LAST_BODY" | head -n 1)"
[[ -n "$sample_id" ]] || fail "sample list does not contain project sample for $nonstandard_project_id"

receive_payload="$(jq -nc '{
  receivedAt: "2026-05-15T00:10:00Z",
  receivedBy: "00000000-0000-0000-0000-000000000105",
  checkPassed: true
}')"
request POST "/samples/$sample_id/receive" 200 "$sample_token" "$receive_payload"
jq -e '.status == "PRE_IN_STOCK"' "$LAST_BODY" >/dev/null || fail "sample receive did not move to PRE_IN_STOCK"

stock_payload="$(jq -nc '{inventoryLocation:"ROOM-A-01",stockedAt:"2026-05-15T00:20:00Z"}')"
request POST "/samples/$sample_id/stock-in" 200 "$sample_token" "$stock_payload"
jq -e '.status == "IN_STOCK" and .inventoryLocation == "ROOM-A-01"' "$LAST_BODY" >/dev/null \
  || fail "sample stock-in did not return IN_STOCK"

request GET "/projects?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "SAMPLE_RECEIVED")' "$LAST_BODY" >/dev/null \
  || fail "sample stock-in did not move project to SAMPLE_RECEIVED"

plan_payload="$(jq -nc --arg project_id "$nonstandard_project_id" '{
  projectId: $project_id,
  requiresConditioning: false,
  plannedStartAt: "2026-05-20T00:00:00Z",
  plannedFinishAt: "2026-05-30T00:00:00Z"
}')"
request POST "/study-plans" 201 "$tech_token" "$plan_payload"
plan_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$nonstandard_project_id" \
  '.projectId == $project_id and .status == "DRAFT" and .requiresConditioning == false' "$LAST_BODY" >/dev/null \
  || fail "study plan creation did not return DRAFT plan"

request POST "/study-plans/$plan_id/submit-review" 200 "$sd_token"
jq -e '.status == "READY_TO_DISPATCH"' "$LAST_BODY" >/dev/null \
  || fail "study plan review did not move to READY_TO_DISPATCH"

request POST "/study-plans/$plan_id/dispatch" 200 "$sd_token"
jq -e '.status == "DISPATCHED"' "$LAST_BODY" >/dev/null || fail "study plan dispatch did not return DISPATCHED"

request GET "/work-orders?page=1&pageSize=100" 200 "$sd_token"
work_order_id="$(jq -er --arg project_id "$nonstandard_project_id" \
  '.items[] | select(.projectId == $project_id) | .id' "$LAST_BODY" | head -n 1)"
[[ -n "$work_order_id" ]] || fail "work order list does not contain project work order for $nonstandard_project_id"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "IN_TESTING")' "$LAST_BODY" >/dev/null \
  || fail "study plan dispatch did not move project to IN_TESTING"

request POST "/work-orders/$work_order_id/pick-sample" 200 "$inspector_token"
jq -e '.status == "SAMPLE_PICKED" and .progressPercent == 5' "$LAST_BODY" >/dev/null \
  || fail "work order sample pickup did not return SAMPLE_PICKED"

request POST "/work-orders/$work_order_id/start" 200 "$inspector_token"
jq -e '.status == "IN_PROGRESS" and .progressPercent == 10' "$LAST_BODY" >/dev/null \
  || fail "work order start did not return IN_PROGRESS"

request POST "/work-orders/$work_order_id/enter-result" 200 "$inspector_token"
jq -e '.status == "RESULT_ENTERED" and .progressPercent == 80' "$LAST_BODY" >/dev/null \
  || fail "work order result entry did not return RESULT_ENTERED"

raw_record_payload="$(jq -nc '{
  templateCode: "RAW_RECORD_STERILITY",
  recordData: {
    testItem: "STERILITY",
    testBasis: "ISO 11737",
    testTime: "2026-05-20",
    testLocation: "Lab A",
    temperatureCelsius: "22",
    humidityPercent: "45",
    method: "DIRECT_INOCULATION"
  },
  attachmentFileIds: []
}')"
request POST "/work-orders/$work_order_id/submit-raw-record" 200 "$inspector_token" "$raw_record_payload"
raw_record_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "REVIEWING" and .templateCode == "RAW_RECORD_STERILITY"' "$LAST_BODY" >/dev/null \
  || fail "raw record submit did not return REVIEWING"

raw_record_review_payload="$(jq -nc '{stage:"QC",outcome:"APPROVED",comment:"Local smoke raw record approved"}')"
request POST "/raw-records/$raw_record_id/review" 200 "$qc_token" "$raw_record_review_payload"
jq -e '.status == "APPROVED"' "$LAST_BODY" >/dev/null || fail "raw record review did not return APPROVED"

request GET "/work-orders?page=1&pageSize=100" 200 "$sd_token"
jq -e --arg id "$work_order_id" \
  '.items[] | select(.id == $id and .status == "COMPLETED" and .progressPercent == 100)' "$LAST_BODY" >/dev/null \
  || fail "raw record approval did not complete work order"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "REPORTING")' "$LAST_BODY" >/dev/null \
  || fail "completed work order did not move project to REPORTING"

locked_payload="$(submission_payload "$suffix-locked")"
request PATCH "/submissions/$nonstandard_submission_id" 409 "$portal_token" "$locked_payload"
jq -e '.code == "FIELD_CHANGE_REQUIRES_CHANGE_REQUEST"' "$LAST_BODY" >/dev/null \
  || fail "signed contract did not lock submission change fields"

audit_count="$(db_scalar "select count(*) from audit_log where entity_id = '$submission_id' and action_code = 'SUBMISSION_CREATE' and result = 'SUCCESS';")"
[[ "$audit_count" -ge 1 ]] || fail "expected at least one successful SUBMISSION_CREATE audit row, got $audit_count"
printf 'OK: audit_log has %s successful SUBMISSION_CREATE rows for %s\n' "$audit_count" "$submission_id"

printf 'LOCAL COMPOSE SMOKE PASSED: submission %s project %s quotation %s contract %s plan %s work_order %s raw_record %s\n' \
  "$submission_id" "$project_id" "$quotation_id" "$contract_id" "$plan_id" "$work_order_id" "$raw_record_id"
