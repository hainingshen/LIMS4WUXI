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
need cmp
need sha256sum
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

request_multipart() {
  local method="$1"
  local path="$2"
  local expected="$3"
  local token="${4:-}"
  shift 4
  local status
  local args=(-sS -o "$LAST_BODY" -w "%{http_code}" -X "$method" "$API_BASE$path")

  if [[ -n "$token" ]]; then
    args+=(-H "Authorization: Bearer $token")
  fi

  status="$(curl "${args[@]}" "$@" || true)"
  printf '%s %s -> %s\n' "$method" "$path" "$status" >&2
  [[ "$status" == "$expected" ]] || fail "expected $expected for $method $path, got $status"
}

minio_fetch_object() {
  local object_key="$1"
  local output_path="$2"

  "${DOCKER_CMD[@]}" compose exec -T minio sh -lc \
    "mc alias set local http://127.0.0.1:9000 \"\$MINIO_ROOT_USER\" \"\$MINIO_ROOT_PASSWORD\" >/dev/null && mc cat \"local/lims-files/$object_key\"" \
    >"$output_path" \
    || fail "MinIO object $object_key was not readable"
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
portal_org_id="$(jq -er '.customerOrgId' "$LAST_BODY")"

request GET "/auth/me" 200 "$internal_token"
jq -e '.clientType == "INTERNAL"' "$LAST_BODY" >/dev/null || fail "internal auth/me did not return INTERNAL clientType"

request GET "/auth/me" 200 "$inspector_token"
inspector_user_id="$(jq -er '.id' "$LAST_BODY")"

request GET "/auth/me" 200 "$finance_token"
finance_user_id="$(jq -er '.id' "$LAST_BODY")"

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

request GET "/system-configs" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "system config list is empty"

request GET "/system-configs" 403 "$portal_token"
jq -e '.code == "FORBIDDEN"' "$LAST_BODY" >/dev/null || fail "portal system config list did not return FORBIDDEN"

system_config_key="LOCAL_SMOKE_CONFIG_$suffix"
system_config_payload="$(jq -nc --arg key "$system_config_key" '{
  configKey: $key,
  configValue: "true",
  valueType: "BOOLEAN",
  category: "LOCAL_SMOKE",
  editableByRole: "SYSTEM_ADMIN",
  descriptionZh: "Local smoke config"
}')"
request POST "/system-configs" 200 "$admin_token" "$system_config_payload"
jq -e --arg key "$system_config_key" '.configKey == $key and .valueType == "BOOLEAN"' "$LAST_BODY" >/dev/null \
  || fail "system config upsert did not return saved config"

request GET "/business-rules" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "business rule list is empty"

business_rule_code="LOCAL_SMOKE_RULE_$suffix"
business_rule_payload="$(jq -nc --arg code "$business_rule_code" '{
  ruleCode: $code,
  nameZh: "Local Smoke Rule",
  triggerType: "LOCAL_SMOKE",
  conditionExpr: "true == true",
  actionCode: "LOCAL_SMOKE_ACTION",
  actionPayload: {stage: "LOCAL"},
  priority: 999,
  enabled: true,
  note: "Local smoke rule"
}')"
request POST "/business-rules" 200 "$admin_token" "$business_rule_payload"
jq -e --arg code "$business_rule_code" '.ruleCode == $code and .enabled == true and .actionPayload.stage == "LOCAL"' "$LAST_BODY" >/dev/null \
  || fail "business rule upsert did not return saved rule"

request GET "/audit-logs?page=1&pageSize=50" 200 "$admin_token"
jq -e '.items and (.total >= 1) and (.items[] | select(.actionCode == "SYSTEM_CONFIG" and .result == "SUCCESS"))' "$LAST_BODY" >/dev/null \
  || fail "audit logs did not contain successful SYSTEM_CONFIG entry"

request GET "/holidays" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "holiday list is empty"

holiday_id="00000000-0000-4000-8000-$(printf '%012d' "$((suffix % 1000000000000))")"
holiday_payload="$(jq -nc --arg id "$holiday_id" '{
  id: $id,
  date: "2026-12-31",
  type: "HOLIDAY",
  name: "Local Smoke Holiday"
}')"
request POST "/holidays" 200 "$admin_token" "$holiday_payload"
jq -e '.date == "2026-12-31" and .type == "HOLIDAY"' "$LAST_BODY" >/dev/null \
  || fail "holiday upsert did not return saved holiday"

request GET "/organization-units" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "organization unit list is empty"
org_unit_id="$(jq -er '.[0].id' "$LAST_BODY")"

request GET "/users?page=1&pageSize=20" 200 "$admin_token"
jq -e '.items and (.total >= 1)' "$LAST_BODY" >/dev/null || fail "internal user list is empty"

request GET "/roles" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "role list is empty"
jq -e 'all(.[]; (.side == "INTERNAL" or .side == "PORTAL"))' "$LAST_BODY" >/dev/null \
  || fail "role list did not expose side"

internal_user_username="internal.smoke.$suffix"
internal_user_password="Temp12345!"
internal_user_new_password="Reset12345!"
internal_user_payload="$(jq -nc \
  --arg username "$internal_user_username" \
  --arg org_unit_id "$org_unit_id" \
  --arg suffix "$suffix" \
  --arg password "$internal_user_password" '{
    username: $username,
    displayName: "Local Smoke Internal User",
    email: ("internal-smoke-" + $suffix + "@example.com"),
    organizationUnitId: $org_unit_id,
    password: $password,
    roleCodes: ["INSPECTOR"]
  }')"
request POST "/users" 201 "$admin_token" "$internal_user_payload"
internal_user_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg username "$internal_user_username" '.username == $username and .status == "ACTIVE" and .roleCodes == ["INSPECTOR"]' "$LAST_BODY" >/dev/null \
  || fail "internal user creation did not return active inspector"

internal_user_update_payload="$(jq -nc --arg org_unit_id "$org_unit_id" --arg suffix "$suffix" '{
  displayName: "Local Smoke Internal User Updated",
  email: ("internal-smoke-updated-" + $suffix + "@example.com"),
  organizationUnitId: $org_unit_id,
  roleCodes: ["INSPECTOR"]
}')"
request PATCH "/users/$internal_user_id" 200 "$admin_token" "$internal_user_update_payload"
jq -e '.displayName == "Local Smoke Internal User Updated" and .roleCodes == ["INSPECTOR"]' "$LAST_BODY" >/dev/null \
  || fail "internal user update did not return updated profile"

created_internal_token="$(login internal "$internal_user_username" "$internal_user_password")"
request GET "/auth/me" 200 "$created_internal_token"
jq -e '.roleCodes == ["INSPECTOR"]' "$LAST_BODY" >/dev/null \
  || fail "created internal user did not login with INSPECTOR role"

request POST "/users/$internal_user_id/roles" 200 "$admin_token" '{"roleCodes":["QC"]}'
jq -e '.roleCodes == ["QC"]' "$LAST_BODY" >/dev/null || fail "internal user role replacement did not return QC"

created_qc_token="$(login internal "$internal_user_username" "$internal_user_password")"
request GET "/auth/me" 200 "$created_qc_token"
jq -e '.roleCodes == ["QC"]' "$LAST_BODY" >/dev/null \
  || fail "created internal user did not login with replaced QC role"

request POST "/users/$internal_user_id/status" 200 "$admin_token" '{"status":"INACTIVE"}'
jq -e '.status == "INACTIVE"' "$LAST_BODY" >/dev/null || fail "internal user disable did not return INACTIVE"
disabled_login_payload="$(jq -nc --arg username "$internal_user_username" --arg password "$internal_user_password" \
  '{username:$username,password:$password}')"
request POST "/auth/internal/login" 401 "" "$disabled_login_payload"
jq -e '.code == "INVALID_CREDENTIALS"' "$LAST_BODY" >/dev/null || fail "disabled internal user login did not fail"

request POST "/users/$internal_user_id/reset-password" 200 "$admin_token" \
  "$(jq -nc --arg password "$internal_user_new_password" '{password:$password}')"
request POST "/users/$internal_user_id/status" 200 "$admin_token" '{"status":"ACTIVE"}'
request POST "/auth/internal/login" 401 "" "$disabled_login_payload"
jq -e '.code == "INVALID_CREDENTIALS"' "$LAST_BODY" >/dev/null || fail "old internal user password still works after reset"
created_reset_token="$(login internal "$internal_user_username" "$internal_user_new_password")"
request GET "/auth/me" 200 "$created_reset_token"
jq -e --arg username "$internal_user_username" '.username == $username and .roleCodes == ["QC"]' "$LAST_BODY" >/dev/null \
  || fail "reset password internal user login did not return QC role"

request POST "/roles/ETHICS_COMMITTEE/status" 200 "$admin_token" '{"status":"INACTIVE"}'
jq -e '.roleCode == "ETHICS_COMMITTEE" and .status == "INACTIVE"' "$LAST_BODY" >/dev/null \
  || fail "role status update did not disable ETHICS_COMMITTEE"
disabled_role_user_payload="$(jq -nc --arg suffix "$suffix" --arg password "$internal_user_password" '{
  username: ("internal.ethics.smoke." + $suffix),
  displayName: "Local Smoke Disabled Role User",
  password: $password,
  roleCodes: ["ETHICS_COMMITTEE"]
}')"
request POST "/users" 400 "$admin_token" "$disabled_role_user_payload"
jq -e '.code == "INTERNAL_USER_ROLE_INVALID"' "$LAST_BODY" >/dev/null \
  || fail "disabled internal role was still assignable"
request POST "/roles/ETHICS_COMMITTEE/status" 200 "$admin_token" '{"status":"ACTIVE"}'
jq -e '.roleCode == "ETHICS_COMMITTEE" and .status == "ACTIVE"' "$LAST_BODY" >/dev/null \
  || fail "role status update did not re-enable ETHICS_COMMITTEE"

request GET "/audit-logs?page=1&pageSize=200" 200 "$admin_token"
for action_code in USER_CREATE USER_UPDATE USER_ROLE_REPLACE USER_STATUS_UPDATE USER_PASSWORD_RESET ROLE_STATUS_UPDATE
do
  jq -e --arg action "$action_code" '.items[] | select(.actionCode == $action and .result == "SUCCESS")' "$LAST_BODY" >/dev/null \
    || fail "audit logs did not contain successful $action_code entry"
done

request GET "/legacy-status-mappings" 200 "$admin_token"
jq -e 'type == "array" and length >= 1' "$LAST_BODY" >/dev/null || fail "legacy status mapping list is empty"

legacy_status_text="LOCAL_SMOKE_STATUS_$suffix"
legacy_mapping_payload="$(jq -nc --arg legacy_status_text "$legacy_status_text" '{
  legacyEntityType: "SUBMISSION",
  legacyStatusText: $legacy_status_text,
  targetEntityType: "SUBMISSION",
  targetStatus: "SUBMITTED",
  migrationMode: "MANUAL_REVIEW",
  qualityScoreDelta: -5,
  note: "Local smoke legacy status"
}')"
request POST "/legacy-status-mappings" 200 "$admin_token" "$legacy_mapping_payload"
jq -e --arg legacy_status_text "$legacy_status_text" '.legacyStatusText == $legacy_status_text and .migrationMode == "MANUAL_REVIEW"' "$LAST_BODY" >/dev/null \
  || fail "legacy status mapping upsert did not return saved mapping"

payload="$(submission_payload "$suffix")"
request POST "/submissions" 201 "$portal_token" "$payload"
submission_id="$(jq -er '.id' "$LAST_BODY")"
jq -e '.status == "DRAFT"' "$LAST_BODY" >/dev/null || fail "created submission is not DRAFT"
jq -e --arg org_id "$portal_org_id" '.customerOrgId == $org_id' "$LAST_BODY" >/dev/null \
  || fail "created portal submission is not bound to portal customer org $portal_org_id"

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

request GET "/projects?page=1&pageSize=20" 200 "$portal_token"
jq -e --arg id "$project_id" '.items | map(.id) | index($id) != null' "$LAST_BODY" >/dev/null \
  || fail "portal project list does not contain own project $project_id"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "portal project list returned data outside portal customer org $portal_org_id"

blocker_expected_at="$(date -u -d '+1 day' '+%Y-%m-%dT%H:%M:%SZ')"
blocker_payload="$(jq -nc --arg project_id "$project_id" --arg expected_at "$blocker_expected_at" '{
  projectId: $project_id,
  blockerType: "CUSTOMER_CONFIRMATION",
  severity: "HIGH",
  description: "Local smoke customer confirmation blocker",
  impactScope: "Local smoke project progression",
  expectedRecoveryAt: $expected_at,
  ownerUserId: "00000000-0000-0000-0000-000000000101",
  collaboratorIds: []
}')"
request POST "/work-center/blockers" 201 "$admin_token" "$blocker_payload"
blocker_id="$(jq -er '.id' "$LAST_BODY")"
jq -e --arg project_id "$project_id" \
  '.projectId == $project_id and .status == "OPEN" and .severity == "HIGH"' "$LAST_BODY" >/dev/null \
  || fail "work center blocker creation did not return open high blocker"

request GET "/work-center/summary" 200 "$admin_token"
jq -e '.riskProjects >= 1' "$LAST_BODY" >/dev/null || fail "work center summary did not count risk project"

request GET "/work-center/projects?filter=risk&page=1&pageSize=100" 200 "$admin_token"
jq -e --arg id "$project_id" \
  '.items[] | select(.projectId == $id and .healthStatus == "RISK" and (.mainBlocker | contains("Local smoke customer confirmation blocker")))' "$LAST_BODY" >/dev/null \
  || fail "work center risk project list did not show created blocker"

request GET "/work-center/trends" 200 "$admin_token"
jq -e '.[] | select(.metricCode == "openBlockers" and (.value | tonumber) >= 1)' "$LAST_BODY" >/dev/null \
  || fail "work center trends did not count open blockers"

request PATCH "/work-center/blockers/$blocker_id" 200 "$admin_token" '{"status":"IN_PROGRESS"}'
jq -e '.status == "IN_PROGRESS"' "$LAST_BODY" >/dev/null || fail "work center blocker did not move to IN_PROGRESS"

request POST "/work-center/blockers/$blocker_id/close" 400 "$admin_token" '{}'
jq -e '.code == "BLOCKER_RESOLUTION_REQUIRED"' "$LAST_BODY" >/dev/null \
  || fail "closing blocker without resolution did not require resolution"

request POST "/work-center/blockers/$blocker_id/close" 200 "$admin_token" '{"resolution":"Local smoke blocker resolved"}'
jq -e '.status == "CLOSED" and .resolution == "Local smoke blocker resolved"' "$LAST_BODY" >/dev/null \
  || fail "work center blocker close did not return closed blocker"

request GET "/work-center/blockers?page=1&pageSize=100" 200 "$admin_token"
jq -e --arg id "$blocker_id" \
  '.items[] | select(.id == $id and .status == "CLOSED")' "$LAST_BODY" >/dev/null \
  || fail "work center blocker list did not show closed blocker"

request GET "/work-center/projects?filter=normal&page=1&pageSize=100" 200 "$admin_token"
jq -e --arg id "$project_id" \
  '.items[] | select(.projectId == $id and .healthStatus == "NORMAL")' "$LAST_BODY" >/dev/null \
  || fail "closing blocker did not restore project to normal health"

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

request GET "/contracts?page=1&pageSize=100" 200 "$portal_token"
jq -e --arg id "$contract_id" \
  '.items[] | select(.id == $id and .status == "PENDING_SIGN")' "$LAST_BODY" >/dev/null \
  || fail "portal contract list does not show own pending contract"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "portal contract list returned data outside portal customer org $portal_org_id"

signed_payload="$(jq -nc '{signedAt:"2026-05-15T00:00:00Z"}')"
request POST "/contracts/$contract_id/mark-signed" 200 "$portal_token" "$signed_payload"
jq -e '.status == "SIGNED" and .signedAt == "2026-05-15T00:00:00Z"' "$LAST_BODY" >/dev/null \
  || fail "portal contract signing did not return signed contract"

request GET "/contracts?page=1&pageSize=100" 200 "$portal_token"
jq -e --arg id "$contract_id" \
  '.items[] | select(.id == $id and .status == "SIGNED")' "$LAST_BODY" >/dev/null \
  || fail "portal contract list does not show signed contract"
jq -e --arg org_id "$portal_org_id" 'all(.items[]?; .customerOrgId == $org_id)' "$LAST_BODY" >/dev/null \
  || fail "portal signed contract list returned data outside portal customer org $portal_org_id"

request GET "/projects?page=1&pageSize=20" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "CONTRACTED")' "$LAST_BODY" >/dev/null \
  || fail "portal signed contract did not move project to CONTRACTED"

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

sample_photo_path="$TMP_DIR/local-smoke-sample-photo.jpg"
printf 'local smoke sample photo %s\n' "$suffix" >"$sample_photo_path"
sample_photo_sha256="$(sha256sum "$sample_photo_path" | awk '{print $1}')"
sample_photo_size="$(wc -c <"$sample_photo_path" | tr -d ' ')"
request_multipart POST "/samples/$sample_id/photos?note=Local%20smoke%20receiving%20photo" 201 "$sample_token" \
  -F "file=@$sample_photo_path;type=image/jpeg;filename=local-smoke-sample-photo.jpg"
sample_photo_id="$(jq -er '.id' "$LAST_BODY")"
sample_photo_file_id="$(jq -er '.fileId' "$LAST_BODY")"
sample_photo_object_key="$(jq -er '.objectKey' "$LAST_BODY")"
jq -e \
  --arg sample_id "$sample_id" \
  --arg file_sha "$sample_photo_sha256" \
  --argjson file_size "$sample_photo_size" \
  '.sampleId == $sample_id
    and .fileName == "local-smoke-sample-photo.jpg"
    and .sha256 == $file_sha
    and .sizeBytes == $file_size
    and (.objectKey | startswith("samples/" + $sample_id + "/photos/"))' "$LAST_BODY" >/dev/null \
  || fail "sample photo upload did not return expected metadata"

metadata_count="$(db_scalar "select count(*) from file_metadata where id = '$sample_photo_file_id' and object_key = '$sample_photo_object_key' and sha256 = '$sample_photo_sha256' and size_bytes = $sample_photo_size;")"
[[ "$metadata_count" == "1" ]] || fail "sample photo file_metadata row was not persisted"
photo_count="$(db_scalar "select count(*) from sample_photo where id = '$sample_photo_id' and sample_id = '$sample_id' and file_id = '$sample_photo_file_id' and note = 'Local smoke receiving photo';")"
[[ "$photo_count" == "1" ]] || fail "sample photo business association was not persisted"
minio_photo_path="$TMP_DIR/minio-sample-photo.jpg"
minio_fetch_object "$sample_photo_object_key" "$minio_photo_path"
cmp -s "$sample_photo_path" "$minio_photo_path" || fail "MinIO sample photo bytes do not match uploaded file"

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

request GET "/work-orders?page=1&pageSize=100" 200 "$inspector_token"
jq -e --arg user_id "$inspector_user_id" 'all(.items[]?; .ownerUserId == $user_id)' "$LAST_BODY" >/dev/null \
  || fail "inspector work order list returned work orders owned by another user"

request GET "/work-orders?page=1&pageSize=100" 200 "$finance_token"
jq -e '.total == 0 and (.items | length == 0)' "$LAST_BODY" >/dev/null \
  || fail "finance user should not see work orders"

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

request GET "/raw-records?page=1&pageSize=100" 200 "$inspector_token"
jq -e --arg user_id "$inspector_user_id" 'all(.items[]?; .testerId == $user_id)' "$LAST_BODY" >/dev/null \
  || fail "inspector raw record list returned records from another tester"

request GET "/raw-records?page=1&pageSize=100" 200 "$finance_token"
jq -e '.total == 0 and (.items | length == 0)' "$LAST_BODY" >/dev/null \
  || fail "finance user should not see raw records"

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

request GET "/projects?page=1&pageSize=100" 200 "$portal_token"
cp "$LAST_BODY" "$TMP_DIR/portal-projects.json"
request GET "/reports?page=1&pageSize=100" 200 "$portal_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "ISSUED_PAYMENT_LOCKED" and .paymentGateStatus == "LOCKED")' "$LAST_BODY" >/dev/null \
  || fail "portal report list does not show own locked issued report"
jq -e --slurpfile projects "$TMP_DIR/portal-projects.json" \
  '($projects[0].items | map(.id)) as $ids | all(.items[]?; .projectId as $project_id | ($ids | index($project_id) != null))' \
  "$LAST_BODY" >/dev/null \
  || fail "portal report list returned data outside the visible portal project"

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

request GET "/reports?page=1&pageSize=100" 200 "$finance_token"
jq -e '.total == 0 and (.items | length == 0)' "$LAST_BODY" >/dev/null \
  || fail "finance user should not see report workbench records"

request GET "/projects?page=1&pageSize=100" 200 "$portal_token"
cp "$LAST_BODY" "$TMP_DIR/portal-projects.json"
request GET "/reports?page=1&pageSize=100" 200 "$portal_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "GREEN_RELEASED" and .paymentGateStatus == "GREEN_UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "portal report list does not show green-unlocked report"
jq -e --slurpfile projects "$TMP_DIR/portal-projects.json" \
  '($projects[0].items | map(.id)) as $ids | all(.items[]?; .projectId as $project_id | ($ids | index($project_id) != null))' \
  "$LAST_BODY" >/dev/null \
  || fail "portal green-unlocked report list returned data outside the visible portal project"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .paymentStatus != "PAID")' "$LAST_BODY" >/dev/null \
  || fail "green channel should not mark project paid"

request GET "/report-delivery-logs?reportId=$report_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "GREEN_CHANNEL_RELEASE" and .paymentGateStatus == "GREEN_UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "green channel approval did not write delivery log"

request GET "/reports/$report_id/official-pdf" 200 "$portal_token"
grep -q "%PDF-1.4" "$LAST_BODY" || fail "green channel official PDF did not return PDF bytes"

request GET "/report-delivery-logs?reportId=$report_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "OFFICIAL_DOWNLOAD" and .paymentGateStatus == "GREEN_UNLOCKED" and .clientType == "PORTAL")' "$LAST_BODY" >/dev/null \
  || fail "green channel official PDF download did not write portal delivery log"

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

request GET "/payment-receipts?page=1&pageSize=100" 200 "$finance_token"
jq -e --arg id "$payment_id" '.items[] | select(.id == $id and .status == "RECORDED")' "$LAST_BODY" >/dev/null \
  || fail "finance payment receipt list did not contain recorded payment"

request GET "/payment-receipts?page=1&pageSize=100" 200 "$inspector_token"
jq -e '.total == 0 and (.items | length == 0)' "$LAST_BODY" >/dev/null \
  || fail "inspector should not see payment receipts"

request GET "/reports/$report_id/official-pdf" 200 "$portal_token"
grep -q "%PDF-1.4" "$LAST_BODY" || fail "official PDF download did not return PDF bytes"

request GET "/reports?page=1&pageSize=100" 200 "$report_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "RELEASED" and .paymentGateStatus == "UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not release report"

request GET "/reports?page=1&pageSize=100" 200 "$portal_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.id == $id and .status == "RELEASED" and .paymentGateStatus == "UNLOCKED")' "$LAST_BODY" >/dev/null \
  || fail "portal report list does not show paid released report"

request GET "/projects?page=1&pageSize=100" 200 "$internal_token"
jq -e --arg id "$nonstandard_project_id" \
  '.items[] | select(.id == $id and .status == "RELEASED" and .paymentStatus == "PAID")' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not release project"

request GET "/report-delivery-logs?reportId=$report_id&page=1&pageSize=20" 200 "$admin_token"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "PAYMENT_UNLOCK_RELEASE" and .paymentGateStatus == "UNLOCKED" and .clientType == "INTERNAL")' "$LAST_BODY" >/dev/null \
  || fail "payment receipt did not write unlock release log"
jq -e --arg id "$report_id" \
  '.items[] | select(.reportId == $id and .deliveryType == "OFFICIAL_DOWNLOAD" and .paymentGateStatus == "UNLOCKED" and .clientType == "PORTAL")' "$LAST_BODY" >/dev/null \
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

request GET "/archive-items?projectId=$nonstandard_project_id&page=1&pageSize=20" 200 "$archive_token"
jq -e --arg id "$archive_item_id" \
  '.items[] | select(.id == $id and .status == "ARCHIVED")' "$LAST_BODY" >/dev/null \
  || fail "archive manager scoped list did not contain created item"

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

printf 'LOCAL COMPOSE SMOKE PASSED: customer_org %s portal_user %s system_config %s business_rule %s holiday %s legacy_status %s submission %s project %s blocker %s quotation %s contract %s sample_photo %s plan %s work_order %s raw_record %s report %s green_channel %s payment %s registry_task %s archive_item %s inventory_batch %s device_booking %s device_file %s retention %s\n' \
  "$customer_org_id" "$portal_user_id" "$system_config_key" "$business_rule_code" "$holiday_id" "$legacy_status_text" "$submission_id" "$project_id" "$blocker_id" "$quotation_id" "$contract_id" "$sample_photo_id" "$plan_id" "$work_order_id" "$raw_record_id" "$report_id" "$green_request_id" "$payment_id" "$registry_task_id" "$archive_item_id" "$inventory_batch_id" "$device_booking_id" "$device_file_id" "$retention_id"
