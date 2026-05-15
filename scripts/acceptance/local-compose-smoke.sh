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
admin_token="$(login internal internal.admin Internal123!)"
internal_token="$(login internal internal.sales Sales123!)"
tech_token="$(login internal internal.tech Sales123!)"
business_token="$(login internal internal.business Sales123!)"
sample_token="$(login internal internal.sample Sales123!)"
sd_token="$(login internal internal.sd Sales123!)"
inspector_token="$(login internal internal.inspector Sales123!)"
qc_token="$(login internal internal.qc Sales123!)"
qa_token="$(login internal internal.qa Sales123!)"
report_token="$(login internal internal.report_preparer Sales123!)"
signatory_token="$(login internal internal.authorized_signatory Sales123!)"
dept_token="$(login internal internal.dept_manager Sales123!)"
general_token="$(login internal internal.general_manager Sales123!)"
finance_token="$(login internal internal.finance Sales123!)"
equipment_token="$(login internal internal.equipment_manager Sales123!)"
archive_token="$(login internal internal.archive Sales123!)"
inventory_token="$(login internal internal.inventory Sales123!)"
suffix="$(date +%s)"

request GET "/auth/me" 200 "$portal_token"
jq -e '.clientType == "PORTAL"' "$LAST_BODY" >/dev/null || fail "portal auth/me did not return PORTAL clientType"

request GET "/auth/me" 200 "$internal_token"
jq -e '.clientType == "INTERNAL"' "$LAST_BODY" >/dev/null || fail "internal auth/me did not return INTERNAL clientType"

request GET "/dictionaries?dictCode=YES_NO" 200 "$portal_token"
jq -e 'length > 0' "$LAST_BODY" >/dev/null || fail "YES_NO dictionary is empty"

request GET "/form-schemas?formCode=SUBMISSION_VER15&status=PUBLISHED" 200 "$portal_token"
jq -e '.[0].formCode == "SUBMISSION_VER15"' "$LAST_BODY" >/dev/null || fail "SUBMISSION_VER15 schema not found"

request GET "/customer-orgs?page=1&pageSize=20" 200 "$admin_token"
jq -e '.items and (.total >= 1)' "$LAST_BODY" >/dev/null || fail "customer org list did not return seeded data"

request GET "/customer-orgs?page=1&pageSize=20" 403 "$portal_token"
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "portal customer org list did not return FORBIDDEN"

customer_payload="$(jq -nc --arg suffix "$suffix" '{
  nameZh: ("Local Smoke Customer " + $suffix),
  nameEn: ("Local Smoke Customer " + $suffix),
  addressZh: "No. 19 Customer Master Road",
  addressEn: "No. 19 Customer Master Road",
  taxNo: ("TAX-" + $suffix),
  invoiceTitle: ("Local Smoke Customer " + $suffix)
}')"
request POST "/customer-orgs" 200 "$admin_token" "$customer_payload"
customer_org_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg suffix "$suffix" '.nameZh == ("Local Smoke Customer " + $suffix) and .status == "ACTIVE"' "$LAST_BODY" >/dev/null \
  || fail "customer org upsert did not return active customer"

portal_user_payload="$(jq -nc --arg customer_org_id "$customer_org_id" --arg suffix "$suffix" '{
  customerOrgId: $customer_org_id,
  username: ("portal.smoke." + $suffix),
  displayName: "Local Smoke Portal User",
  email: ("portal-smoke-" + $suffix + "@example.com"),
  roleCodes: ["CUSTOMER_APPLICANT"]
}')"
request POST "/portal-users" 201 "$admin_token" "$portal_user_payload"
portal_user_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.roleCodes == ["CUSTOMER_APPLICANT"] and .status == "ACTIVE"' "$LAST_BODY" >/dev/null \
  || fail "portal user creation did not return active applicant"

request GET "/portal-users?customerOrgId=$customer_org_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$portal_user_id" '.items | map(.id) | index($id) != null' "$LAST_BODY" >/dev/null \
  || fail "portal user list does not contain created user"

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
    method: "DIRECT_INOCULATION",
    conclusion: "No growth observed",
    equipmentUsages: [
      {
        name: "Incubator",
        no: "EQ-SMOKE"
      }
    ],
    sterilityResults: [
      {
        sampleName: "Local Smoke Sample",
        conclusion: "无菌"
      }
    ]
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

template_file_id="$(cat /proc/sys/kernel/random/uuid)"
db_scalar "
insert into file_metadata (
  id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
  created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
) values (
  '$template_file_id',
  'local-smoke-sterility-template.docx',
  'reports/templates/$template_file_id.docx',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  1024,
  'sha256-$template_file_id',
  '00000000-0000-0000-0000-000000000101',
  now(),
  '00000000-0000-0000-0000-000000000101',
  now(),
  '00000000-0000-0000-0000-000000000101',
  0,
  'NEW',
  100
)
returning id;
" >/dev/null

template_version="SMOKE-$suffix-${template_file_id:0:8}"
template_payload="$(jq -nc --arg file_id "$template_file_id" --arg template_version "$template_version" '{
  templateCode: "STERILITY_REPORT",
  templateVersion: $template_version,
  templateName: "Local Smoke Sterility Report",
  templateType: "REPORT",
  automationLevel: "A",
  fileId: $file_id,
  legacyTemplateName: "local-smoke-sterility"
}')"
request POST "/report-templates" 201 "$admin_token" "$template_payload"
template_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "DRAFT" and .templateCode == "STERILITY_REPORT"' "$LAST_BODY" >/dev/null \
  || fail "report template creation did not return DRAFT template"

request POST "/report-templates/$template_id/publish" 200 "$admin_token"
jq -e '.status == "PUBLISHED"' "$LAST_BODY" >/dev/null || fail "report template publish did not return PUBLISHED"

report_payload="$(jq -nc --arg project_id "$nonstandard_project_id" --arg work_order_id "$work_order_id" '{
  projectId: $project_id,
  templateCode: "STERILITY_REPORT",
  workOrderIds: [$work_order_id]
}')"
request POST "/reports/generate-draft" 201 "$report_token" "$report_payload"
report_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$nonstandard_project_id" \
  '.projectId == $project_id and .status == "DRAFT" and .paymentGateStatus == "LOCKED"' "$LAST_BODY" >/dev/null \
  || fail "report draft generation did not return DRAFT locked report"

request POST "/reports/$report_id/submit-qc" 200 "$report_token"
jq -e '.status == "QC_REVIEWING"' "$LAST_BODY" >/dev/null || fail "report submit-qc did not return QC_REVIEWING"

report_qc_payload="$(jq -nc '{stage:"QC",outcome:"APPROVED",comment:"Local smoke QC approved"}')"
request POST "/reports/$report_id/review" 200 "$qc_token" "$report_qc_payload"
jq -e '.status == "QA_REVIEWING"' "$LAST_BODY" >/dev/null || fail "report QC review did not return QA_REVIEWING"

report_qa_payload="$(jq -nc '{stage:"QA",outcome:"APPROVED",comment:"Local smoke QA approved"}')"
request POST "/reports/$report_id/review" 200 "$qa_token" "$report_qa_payload"
jq -e '.status == "SD_REVIEWING"' "$LAST_BODY" >/dev/null || fail "report QA review did not return SD_REVIEWING"

report_sd_payload="$(jq -nc '{stage:"SD",outcome:"APPROVED",comment:"Local smoke SD approved"}')"
request POST "/reports/$report_id/review" 200 "$sd_token" "$report_sd_payload"
jq -e '.status == "PENDING_ISSUE"' "$LAST_BODY" >/dev/null || fail "report SD review did not return PENDING_ISSUE"

request POST "/reports/$report_id/issue" 200 "$signatory_token"
jq -e '.status == "ISSUED_PAYMENT_LOCKED" and .paymentGateStatus == "LOCKED"' "$LAST_BODY" >/dev/null \
  || fail "report issue did not return ISSUED_PAYMENT_LOCKED"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "PAYMENT_HOLD")' "$LAST_BODY" >/dev/null \
  || fail "issued report did not move project to PAYMENT_HOLD"

request GET "/reports/$report_id/official-pdf" 423 "$portal_token"
jq -e '.code == "PAYMENT_GATE_LOCKED"' "$LAST_BODY" >/dev/null \
  || fail "locked official PDF did not return PAYMENT_GATE_LOCKED"

green_payload="$(jq -nc --arg project_id "$nonstandard_project_id" --arg report_id "$report_id" '{
  projectId: $project_id,
  reportId: $report_id,
  reason: "Local smoke customer needs the electronic report before payment settlement."
}')"
request POST "/green-channel-requests" 201 "$internal_token" "$green_payload"
green_request_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "DEPT_REVIEWING" and .unlockScope == "ELECTRONIC_REPORT_ONLY"' "$LAST_BODY" >/dev/null \
  || fail "green channel creation did not return DEPT_REVIEWING"

green_dept_payload="$(jq -nc '{stage:"DEPT",outcome:"APPROVED",comment:"Local smoke department approved"}')"
request POST "/green-channel-requests/$green_request_id/approve" 204 "$dept_token" "$green_dept_payload"
request GET "/green-channel-requests?projectId=$nonstandard_project_id&page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$green_request_id" \
  '.items[] | select(.id == $id and .status == "MANAGEMENT_REVIEWING")' "$LAST_BODY" >/dev/null \
  || fail "green channel department approval did not move to MANAGEMENT_REVIEWING"

green_management_payload="$(jq -nc '{stage:"MANAGEMENT",outcome:"APPROVED",comment:"Local smoke management approved"}')"
request POST "/green-channel-requests/$green_request_id/approve" 204 "$general_token" "$green_management_payload"
request GET "/green-channel-requests?projectId=$nonstandard_project_id&page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$green_request_id" \
  '.items[] | select(.id == $id and .status == "APPROVED")' "$LAST_BODY" >/dev/null \
  || fail "green channel final approval did not return APPROVED request"

request GET "/reports?page=1&pageSize=100" 200 "$report_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "GREEN_RELEASED" and .paymentGateStatus == "GREEN_UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "green channel did not unlock report"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .paymentStatus != "PAID")' "$LAST_BODY" >/dev/null \
  || fail "green channel should not mark project paid"

request GET "/report-delivery-logs?reportId=$report_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "GREEN_CHANNEL" and .paymentGateStatus == "GREEN_UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "green channel approval did not write delivery log"

request GET "/reports/$report_id/official-pdf" 200 "$portal_token"
grep -q "%PDF-1.4" "$LAST_BODY" || fail "green channel official PDF did not return PDF bytes"

payment_payload="$(jq -nc --arg project_id "$nonstandard_project_id" --arg report_id "$report_id" '{
  projectId: $project_id,
  reportId: $report_id,
  amount: 1200,
  receivedAt: "2026-05-15T02:00:00Z",
  method: "BANK_TRANSFER",
  note: "Local smoke payment"
}')"
request POST "/payment-receipts" 201 "$finance_token" "$payment_payload"
payment_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "RECORDED" and .amount == 1200' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not return RECORDED"

request GET "/reports/$report_id/official-pdf" 200 "$portal_token"
grep -q "%PDF-1.4" "$LAST_BODY" || fail "official PDF download did not return PDF bytes"

request GET "/reports?page=1&pageSize=100" 200 "$report_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "RELEASED" and .paymentGateStatus == "UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not release report"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "RELEASED" and .paymentStatus == "PAID")' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not release project"

request GET "/report-delivery-logs?reportId=$report_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "DOWNLOAD" and .paymentGateStatus == "UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "official PDF download did not write delivery log"

request GET "/report-registry/search?projectId=$nonstandard_project_id&status=RELEASED&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .status == "RELEASED" and .paymentGateStatus == "UNLOCKED" and (.deliveryCount >= 2) and ((.versions | length) >= 1))' "$LAST_BODY" >/dev/null \
  || fail "report registry did not contain released report with versions and delivery logs"

registry_batch_payload="$(jq -nc --arg report_id "$report_id" '{reportIds:[$report_id],includeDeliveryLog:true}')"
request POST "/report-registry/batch-download" 202 "$admin_token" "$registry_batch_payload"
registry_task_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.taskType == "REPORT_BATCH_DOWNLOAD" and .entityType == "REPORT" and .status == "PENDING"' "$LAST_BODY" >/dev/null \
  || fail "report registry batch download did not create pending report export task"

request GET "/import-export-tasks?page=1&pageSize=100" 200 "$admin_token"
jq -e --arg id "$registry_task_id" \
  '.items[] | select(.id == $id and .taskType == "REPORT_BATCH_DOWNLOAD" and .entityType == "REPORT" and .status == "PENDING")' "$LAST_BODY" >/dev/null \
  || fail "import/export task list did not contain report batch task"

request POST "/import-export-tasks/$registry_task_id/run" 202 "$admin_token"
jq -e '.status == "SUCCESS" and .resultFileId != null and .errorSummary == "SUCCESS: REPORT_BATCH_DOWNLOAD completed"' "$LAST_BODY" >/dev/null \
  || fail "import/export report batch task did not complete with result file"

request GET "/import-export-tasks/$registry_task_id/result-file" 200 "$admin_token"
grep -q "$registry_task_id" "$LAST_BODY" || fail "import/export result file did not contain task id"

archive_payload="$(jq -nc --arg project_id "$nonstandard_project_id" --arg report_id "$report_id" --arg file_id "$template_file_id" '{
  projectId: $project_id,
  entityType: "REPORT",
  entityId: $report_id,
  archiveType: "REPORT_OFFICIAL_PDF",
  fileId: $file_id
}')"
request POST "/archive-items" 201 "$archive_token" "$archive_payload"
archive_item_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$nonstandard_project_id" --arg report_id "$report_id" --arg file_id "$template_file_id" \
  '.projectId == $project_id and .entityType == "REPORT" and .entityId == $report_id and .archiveType == "REPORT_OFFICIAL_PDF" and .fileId == $file_id and .status == "ARCHIVED"' "$LAST_BODY" >/dev/null \
  || fail "archive item creation did not return archived report record"

request GET "/archive-items?projectId=$nonstandard_project_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$archive_item_id" \
  '.items[] | select(.id == $id and .status == "ARCHIVED")' "$LAST_BODY" >/dev/null \
  || fail "archive item list did not contain created item"

inventory_payload="$(jq -nc --arg suffix "$suffix" '{
  batchNo: ("INV-SMOKE-" + $suffix),
  name: "Local Smoke Reference Material",
  casNo: "50-00-0",
  specification: "100 mg",
  manufacturer: "NJXS",
  receivedAt: "2026-05-15",
  expiryDate: "2026-12-31",
  quantity: 10,
  unit: "bottle",
  storageCondition: "ROOM_TEMPERATURE",
  storageLocation: "INV-SMOKE-01"
}')"
request POST "/inventory-batches" 200 "$inventory_token" "$inventory_payload"
inventory_batch_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "IN_STOCK" and .quantity == 10' "$LAST_BODY" >/dev/null \
  || fail "inventory batch creation did not return in-stock batch"

request GET "/inventory-batches?keyword=INV-SMOKE-$suffix&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$inventory_batch_id" \
  '.items[] | select(.id == $id and .status == "IN_STOCK" and .quantity == 10)' "$LAST_BODY" >/dev/null \
  || fail "inventory batch list did not contain created batch"

inventory_txn_payload="$(jq -nc --arg id "$inventory_batch_id" '{
  inventoryBatchId: $id,
  txnType: "USE",
  quantity: 3,
  reason: "Local smoke use"
}')"
request POST "/inventory-transactions" 201 "$inventory_token" "$inventory_txn_payload"
jq -e '.txnType == "USE" and .quantity == 3' "$LAST_BODY" >/dev/null \
  || fail "inventory transaction creation did not return USE transaction"

request GET "/inventory-transactions?inventoryBatchId=$inventory_batch_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$inventory_batch_id" \
  '.items[] | select(.inventoryBatchId == $id and .txnType == "USE" and .quantity == 3)' "$LAST_BODY" >/dev/null \
  || fail "inventory transaction list did not contain created transaction"

request GET "/inventory-batches?keyword=INV-SMOKE-$suffix&page=1&pageSize=20" 200 "$inventory_token"
jq -e --arg id "$inventory_batch_id" \
  '.items[] | select(.id == $id and .quantity == 7 and .status == "IN_STOCK")' "$LAST_BODY" >/dev/null \
  || fail "inventory batch quantity did not decrease after USE transaction"

device_id="$(cat /proc/sys/kernel/random/uuid)"
device_file_source_id="$(cat /proc/sys/kernel/random/uuid)"
db_scalar "
insert into device (
  id, device_no, name, model, manufacturer, location, owner_user_id,
  metrology_required, status, created_at, created_by, updated_at, updated_by,
  version, source_type, data_quality_score
) values (
  '$device_id',
  'DEV-SMOKE-$suffix',
  'Local Smoke Incubator',
  'SMK-37',
  'NJXS',
  'Local Lab A',
  '00000000-0000-0000-0000-000000000116',
  true,
  'ACTIVE',
  now(),
  '00000000-0000-0000-0000-000000000101',
  now(),
  '00000000-0000-0000-0000-000000000101',
  0,
  'NEW',
  100
)
returning id;
" >/dev/null
db_scalar "
insert into file_metadata (
  id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
  created_at, created_by, updated_at, updated_by, version, source_type, data_quality_score
) values (
  '$device_file_source_id',
  'local-smoke-device-calibration.pdf',
  'devices/$device_id/$device_file_source_id.pdf',
  'application/pdf',
  2048,
  'sha256-$device_file_source_id',
  '00000000-0000-0000-0000-000000000116',
  now(),
  '00000000-0000-0000-0000-000000000116',
  now(),
  '00000000-0000-0000-0000-000000000116',
  0,
  'NEW',
  100
)
returning id;
" >/dev/null

request GET "/devices?keyword=DEV-SMOKE-$suffix&page=1&pageSize=20" 200 "$equipment_token"
jq -e --arg id "$device_id" --arg device_no "DEV-SMOKE-$suffix" \
  '.items[] | select(.id == $id and .deviceNo == $device_no and .metrologyRequired == true and .status == "ACTIVE")' "$LAST_BODY" >/dev/null \
  || fail "device list did not contain local smoke device"

booking_start="$(date -u -d '+4 hours' '+%Y-%m-%dT%H:00:00Z')"
booking_end="$(date -u -d '+5 hours' '+%Y-%m-%dT%H:00:00Z')"
booking_payload="$(jq -nc \
  --arg device_id "$device_id" \
  --arg work_order_id "$work_order_id" \
  --arg start_at "$booking_start" \
  --arg end_at "$booking_end" '{
    deviceId: $device_id,
    workOrderId: $work_order_id,
    purpose: "Local smoke internal device booking",
    startAt: $start_at,
    endAt: $end_at
  }')"
request POST "/device-bookings" 201 "$inspector_token" "$booking_payload"
device_booking_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg device_id "$device_id" --arg work_order_id "$work_order_id" \
  '.deviceId == $device_id and .workOrderId == $work_order_id and .bookerType == "INSPECTOR" and .status == "BOOKED"' "$LAST_BODY" >/dev/null \
  || fail "device booking creation did not return booked inspector reservation"

request GET "/device-bookings?deviceId=$device_id&page=1&pageSize=20" 200 "$equipment_token"
jq -e --arg id "$device_booking_id" \
  '.items[] | select(.id == $id and .status == "BOOKED")' "$LAST_BODY" >/dev/null \
  || fail "device booking list did not contain created reservation"

external_booking_start="$(date -u -d '+6 hours' '+%Y-%m-%dT%H:00:00Z')"
external_booking_end="$(date -u -d '+7 hours' '+%Y-%m-%dT%H:00:00Z')"
external_booking_payload="$(jq -nc \
  --arg device_id "$device_id" \
  --arg start_at "$external_booking_start" \
  --arg end_at "$external_booking_end" '{
    deviceId: $device_id,
    purpose: "Local smoke portal device booking",
    startAt: $start_at,
    endAt: $end_at
  }')"
request POST "/device-bookings" 201 "$portal_token" "$external_booking_payload"
external_booking_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.bookerType == "EXTERNAL_USER" and .status == "BOOKED" and .workOrderId == null' "$LAST_BODY" >/dev/null \
  || fail "portal device booking did not return booked external reservation"

override_payload="$(jq -nc \
  --arg start_at "$external_booking_start" \
  --arg end_at "$external_booking_end" '{
    reason: "Local smoke internal priority",
    replacementStartAt: $start_at,
    replacementEndAt: $end_at
  }')"
request POST "/device-bookings/$external_booking_id/override" 200 "$equipment_token" "$override_payload"
jq -e '.status == "NEEDS_RESCHEDULE" and .overrideReason == "Local smoke internal priority"' "$LAST_BODY" >/dev/null \
  || fail "device booking override did not mark external reservation as needs reschedule"

device_file_payload="$(jq -nc --arg file_id "$device_file_source_id" '{
  fileType: "CALIBRATION_CERT",
  fileId: $file_id,
  versionNo: "SMOKE-V1",
  validFrom: "2026-05-01",
  validUntil: "2026-12-31",
  isCurrent: true
}')"
request POST "/devices/$device_id/files" 201 "$equipment_token" "$device_file_payload"
device_file_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg device_id "$device_id" --arg file_id "$device_file_source_id" \
  '.deviceId == $device_id and .fileId == $file_id and .fileType == "CALIBRATION_CERT" and .isCurrent == true and .status == "ACTIVE"' "$LAST_BODY" >/dev/null \
  || fail "device file upload did not return active current file"

request GET "/devices/$device_id/files" 200 "$equipment_token"
jq -e --arg id "$device_file_id" \
  '.[] | select(.id == $id and .versionNo == "SMOKE-V1" and .validUntil == "2026-12-31")' "$LAST_BODY" >/dev/null \
  || fail "device file list did not contain uploaded file"

request POST "/device-files/$device_file_id/disable" 200 "$admin_token"
jq -e '.status == "DISABLED" and .isCurrent == false' "$LAST_BODY" >/dev/null \
  || fail "device file disable did not return disabled file"

retention_payload="$(jq -nc --arg sample_id "$sample_id" --arg project_id "$nonstandard_project_id" --arg report_id "$report_id" '{
  sampleId: $sample_id,
  projectId: $project_id,
  reportId: $report_id,
  retainedQuantity: 2,
  quantityUnit: "pcs",
  storageCondition: "ROOM_TEMPERATURE",
  storageLocation: "RET-SMOKE-01",
  retainUntil: "2026-08-01",
  dispositionPlan: "DESTROY"
}')"
request POST "/sample-retentions" 201 "$sample_token" "$retention_payload"
retention_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "PENDING_REGISTER"' "$LAST_BODY" >/dev/null \
  || fail "sample retention creation did not return PENDING_REGISTER"

request POST "/sample-retentions/$retention_id/register" 200 "$sample_token"
jq -e '.status == "RETAINED"' "$LAST_BODY" >/dev/null \
  || fail "sample retention register did not return RETAINED"

retention_extend_payload="$(jq -nc '{retainUntil:"2026-10-01",reason:"Local smoke retention extension"}')"
request POST "/sample-retentions/$retention_id/extend" 200 "$sample_token" "$retention_extend_payload"
jq -e '.status == "RETAINED" and .retainUntil == "2026-10-01"' "$LAST_BODY" >/dev/null \
  || fail "sample retention extension did not return updated retainUntil"

retention_dispose_payload="$(jq -nc '{disposition:"DESTROY",reason:"Local smoke disposal"}')"
request POST "/sample-retentions/$retention_id/dispose" 200 "$sample_token" "$retention_dispose_payload"
jq -e '.status == "DESTROYED"' "$LAST_BODY" >/dev/null \
  || fail "sample retention disposal did not return DESTROYED"

request GET "/sample-retentions?page=1&pageSize=100" 200 "$sample_token"
jq -e --arg id "$retention_id" \
  '.items[] | select(.id == $id and .status == "DESTROYED")' "$LAST_BODY" >/dev/null \
  || fail "sample retention list does not contain disposed retention"

locked_payload="$(submission_payload "$suffix-locked")"
request PATCH "/submissions/$nonstandard_submission_id" 409 "$portal_token" "$locked_payload"
jq -e '.code == "FIELD_CHANGE_REQUIRES_CHANGE_REQUEST"' "$LAST_BODY" >/dev/null \
  || fail "signed contract did not lock submission change fields"

audit_count="$(db_scalar "select count(*) from audit_log where entity_id = '$submission_id' and action_code = 'SUBMISSION_CREATE' and result = 'SUCCESS';")"
[[ "$audit_count" -ge 1 ]] || fail "expected at least one successful SUBMISSION_CREATE audit row, got $audit_count"
printf 'OK: audit_log has %s successful SUBMISSION_CREATE rows for %s\n' "$audit_count" "$submission_id"

printf 'LOCAL COMPOSE SMOKE PASSED: customer_org %s portal_user %s submission %s project %s quotation %s contract %s plan %s work_order %s raw_record %s report %s green_channel %s payment %s registry_task %s archive_item %s inventory_batch %s device_booking %s device_file %s retention %s\n' \
  "$customer_org_id" "$portal_user_id" "$submission_id" "$project_id" "$quotation_id" "$contract_id" "$plan_id" "$work_order_id" "$raw_record_id" "$report_id" "$green_request_id" "$payment_id" "$registry_task_id" "$archive_item_id" "$inventory_batch_id" "$device_booking_id" "$device_file_id" "$retention_id"
