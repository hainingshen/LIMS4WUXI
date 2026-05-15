#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck source=scripts/local/docker-env.sh
. "$ROOT_DIR/scripts/local/docker-env.sh"
select_docker_cmd || {
  printf 'ERROR: docker daemon is not reachable\n' >&2
  exit 1
}

set -a
# shellcheck disable=SC1091
. ./.env
set +a

scripts/local/cleanup-smoke-data.sh >/dev/null

"${DOCKER_CMD[@]}" compose exec -T postgres psql \
  -v ON_ERROR_STOP=1 \
  -U "${POSTGRES_USER:-lims}" \
  -d "${POSTGRES_DB:-lims}" <<'SQL'
begin;

delete from report_delivery_log where source_type = 'DEMO_SEED';
delete from import_export_task where source_type = 'DEMO_SEED';
delete from archive_item where source_type = 'DEMO_SEED';
delete from sample_retention where source_type = 'DEMO_SEED';
delete from payment_receipt where source_type = 'DEMO_SEED';
delete from green_channel_request where source_type = 'DEMO_SEED';
delete from change_request_item
where change_request_id in (select id from change_request where source_type = 'DEMO_SEED');
delete from change_request where source_type = 'DEMO_SEED';
delete from report_work_order
where report_id in (select id from report where source_type = 'DEMO_SEED')
   or work_order_id in (select id from work_order where source_type = 'DEMO_SEED')
   or raw_record_id in (select id from raw_record where source_type = 'DEMO_SEED');
delete from report_version where source_type = 'DEMO_SEED';
delete from report where source_type = 'DEMO_SEED';
delete from device_booking where source_type = 'DEMO_SEED';
delete from equipment_usage where source_type = 'DEMO_SEED';
delete from raw_record where source_type = 'DEMO_SEED';
delete from work_order where source_type = 'DEMO_SEED';
delete from study_plan where source_type = 'DEMO_SEED';
delete from sample_photo
where sample_id in (select id from sample where source_type = 'DEMO_SEED');
delete from sample where source_type = 'DEMO_SEED';
delete from contract where source_type = 'DEMO_SEED';
delete from quotation_item
where quotation_id in (select id from quotation where source_type = 'DEMO_SEED');
delete from quotation where source_type = 'DEMO_SEED';
delete from work_center_blocker where source_type = 'DEMO_SEED';
delete from project where source_type = 'DEMO_SEED';
delete from submission_test_selection
where submission_id in (select id from submission where source_type = 'DEMO_SEED');
delete from submission where source_type = 'DEMO_SEED';
delete from device_file where source_type = 'DEMO_SEED';
delete from device where source_type = 'DEMO_SEED';
delete from inventory_transaction where source_type = 'DEMO_SEED';
delete from inventory_batch where source_type = 'DEMO_SEED';
delete from report_template where source_type = 'DEMO_SEED';
delete from file_metadata where source_type = 'DEMO_SEED';
delete from user_role
where user_id in (select id from app_user where source_type = 'DEMO_SEED');
delete from app_user where source_type = 'DEMO_SEED';
delete from customer_contact where source_type = 'DEMO_SEED';
delete from manufacturer where source_type = 'DEMO_SEED';
delete from customer_org where source_type = 'DEMO_SEED';
delete from legacy_status_mapping
where legacy_entity_type = 'DEMO_SEED'
   or legacy_status_text like 'DEMO_SEED_%';

insert into customer_org (
  id, biz_no, name_zh, name_en, address_zh, address_en, industry, grade,
  tax_no, invoice_title, status, created_by, updated_by, tenant_org_id,
  source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000001',
  'DEMO-SEED-CUST-001',
  'Demo Seed Sponsor',
  'Demo Seed Sponsor',
  'No. 88 Demo Seed Road, Wuxi',
  'No. 88 Demo Seed Road, Wuxi',
  'MEDICAL_DEVICE',
  'A',
  'DEMO-SEED-TAX-001',
  'Demo Seed Sponsor',
  'ACTIVE',
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000101',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into customer_contact (
  id, biz_no, customer_org_id, contact_name, phone, email, is_primary, status,
  created_by, updated_by, tenant_org_id, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000002',
  'DEMO-SEED-CONTACT-001',
  '018f0000-0000-7000-8000-000000000001',
  'Demo Seed Applicant',
  '13800008888',
  'demo-seed-applicant@example.com',
  true,
  'ACTIVE',
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000101',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into manufacturer (
  id, biz_no, name_zh, name_en, address_zh, address_en, status,
  created_by, updated_by, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000003',
  'DEMO-SEED-MFR-001',
  'Demo Seed Manufacturer',
  'Demo Seed Manufacturer',
  'No. 66 Manufacturing Road',
  'No. 66 Manufacturing Road',
  'ACTIVE',
  '00000000-0000-0000-0000-000000000101',
  '00000000-0000-0000-0000-000000000101',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into app_user (
  id, username, display_name, password_hash, client_type, organization_unit_id,
  customer_org_id, status, created_by, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score, email
) values
  (
    '018f0000-0000-7000-8000-000000000090',
    'portal.demo.seed',
    'Demo Seed Portal Applicant',
    '{bcrypt}$2a$10$bUI33Tb5dPXZgrB6o8/2J.To.pygo8WUIwpQ0gSTGqA43xe5yXMmS',
    'PORTAL',
    null,
    '018f0000-0000-7000-8000-000000000001',
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'portal-demo-seed@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000091',
    'internal.demo.inspector',
    'Demo Seed Inspector',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-inspector@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000092',
    'internal.demo.sales',
    'Demo Seed Sales',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-sales@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000093',
    'internal.demo.report',
    'Demo Seed Report Preparer',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-report@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000094',
    'internal.demo.sample',
    'Demo Seed Sample Manager',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-sample@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000095',
    'internal.demo.equipment',
    'Demo Seed Equipment Manager',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-equipment@example.com'
  ),
  (
    '018f0000-0000-7000-8000-000000000096',
    'internal.demo.admin',
    'Demo Seed Admin',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    '00000000-0000-0000-0000-000000000a01',
    null,
    'ACTIVE',
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000101',
    null,
    'DEMO_SEED',
    'DEMO_SEED',
    100,
    'internal-demo-admin@example.com'
  );

insert into user_role (user_id, role_code) values
  ('018f0000-0000-7000-8000-000000000090', 'CUSTOMER_APPLICANT'),
  ('018f0000-0000-7000-8000-000000000091', 'INSPECTOR'),
  ('018f0000-0000-7000-8000-000000000092', 'SALES_REP'),
  ('018f0000-0000-7000-8000-000000000093', 'REPORT_PREPARER'),
  ('018f0000-0000-7000-8000-000000000094', 'SAMPLE_MANAGER'),
  ('018f0000-0000-7000-8000-000000000095', 'EQUIPMENT_MANAGER'),
  ('018f0000-0000-7000-8000-000000000096', 'SYSTEM_ADMIN');

insert into file_metadata (
  id, file_name, object_key, content_type, size_bytes, sha256, uploaded_by,
  created_by, updated_by, source_type, migration_batch, data_quality_score
) values
  ('018f0000-0000-7000-8000-000000000102', 'demo-seed-report-template.docx', 'demo-seed/templates/sterility-report.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 1024, 'demo-seed-template', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', 'DEMO_SEED', 'DEMO_SEED', 100),
  ('018f0000-0000-7000-8000-000000000112', 'demo-seed-report-v1.docx', 'demo-seed/reports/TR260408-03.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 2048, 'demo-seed-report-docx', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', 'DEMO_SEED', 'DEMO_SEED', 100),
  ('018f0000-0000-7000-8000-000000000113', 'demo-seed-report-v1.pdf', 'demo-seed/reports/TR260408-03.pdf', 'application/pdf', 4096, 'demo-seed-report-pdf', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', 'DEMO_SEED', 'DEMO_SEED', 100),
  ('018f0000-0000-7000-8000-000000000115', 'demo-seed-green-report.pdf', 'demo-seed/reports/TR260408-04.pdf', 'application/pdf', 4096, 'demo-seed-green-report-pdf', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', '018f0000-0000-7000-8000-000000000093', 'DEMO_SEED', 'DEMO_SEED', 100),
  ('018f0000-0000-7000-8000-000000000152', 'demo-seed-device-calibration.pdf', 'demo-seed/devices/incubator-calibration.pdf', 'application/pdf', 2048, 'demo-seed-device-file', '018f0000-0000-7000-8000-000000000095', '018f0000-0000-7000-8000-000000000095', '018f0000-0000-7000-8000-000000000095', 'DEMO_SEED', 'DEMO_SEED', 100),
  ('018f0000-0000-7000-8000-000000000172', 'demo-seed-report-export.zip', 'demo-seed/exports/report-batch.zip', 'application/zip', 8192, 'demo-seed-export', '018f0000-0000-7000-8000-000000000096', '018f0000-0000-7000-8000-000000000096', '018f0000-0000-7000-8000-000000000096', 'DEMO_SEED', 'DEMO_SEED', 100);

insert into report_template (
  id, template_code, template_version, template_name, template_type,
  automation_level, file_id, status, published_at, published_by,
  created_by, updated_by, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000101',
  'STERILITY_REPORT_V1',
  '1.0.0',
  'Demo Seed Sterility Report Template',
  'REPORT',
  'A',
  '018f0000-0000-7000-8000-000000000102',
  'PUBLISHED',
  '2026-04-08T01:30:00Z',
  '018f0000-0000-7000-8000-000000000093',
  '018f0000-0000-7000-8000-000000000093',
  '018f0000-0000-7000-8000-000000000093',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into submission (
  id, biz_no, customer_org_id, applicant_user_id, status, created_at, created_by,
  updated_at, updated_by, assigned_sales_user_id, customer_contact_id, manufacturer_id,
  official_report_email, shipping_info, requires_glp,
  requires_hard_copy_of_electronic_report, report_language, protocol_language,
  submitted_quantity_text, sample_disposition, form_code, schema_version, data_json,
  tenant_org_id, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000011',
  'DEMO-SEED-SUB-001',
  '018f0000-0000-7000-8000-000000000001',
  '018f0000-0000-7000-8000-000000000090',
  'ACCEPTED',
  '2026-03-09T01:00:00Z',
  '018f0000-0000-7000-8000-000000000090',
  '2026-03-09T02:00:00Z',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000002',
  '018f0000-0000-7000-8000-000000000003',
  'demo-seed-report@example.com',
  'Ship to Demo Seed Sponsor',
  false,
  false,
  'CHS',
  'CHS',
  '2 boxes',
  'RETURN_UNUSED',
  'SUBMISSION_VER15',
  1,
  '{"sponsorNameZh":"Demo Seed Sponsor","sampleNameZh":"Demo Seed Hydrocolloid Dressing","lotBatch":"DEMO-SEED-LOT-001","intendedUse":"Demo seed validation"}'::jsonb,
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into project (
  id, biz_no, submission_id, customer_org_id, project_type, qualification_type,
  service_type, requires_glp, requires_animal_test, requires_ethics_review,
  requires_nonstandard_review, business_owner_id, technical_owner_id, sd_user_id,
  status, planned_finish_at, health_status, health_score, payment_status,
  created_at, created_by, updated_at, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000021',
  'DEMO-SEED-P-001',
  '018f0000-0000-7000-8000-000000000011',
  '018f0000-0000-7000-8000-000000000001',
  'NON_STANDARD',
  'CMA',
  'NORMAL',
  false,
  false,
  false,
  true,
  '00000000-0000-0000-0000-000000000104',
  '00000000-0000-0000-0000-000000000103',
  '00000000-0000-0000-0000-000000000106',
  'PAYMENT_HOLD',
  '2026-04-10T09:00:00Z',
  'WARNING',
  78,
  'UNPAID',
  '2026-03-09T02:10:00Z',
  '018f0000-0000-7000-8000-000000000092',
  '2026-04-08T03:00:00Z',
  '018f0000-0000-7000-8000-000000000093',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into sample (
  id, biz_no, project_id, submission_id, sample_name_zh, sample_name_en,
  sterilization_state, cas_code, model_ref, size_spec, lot_batch, raw_material,
  packaging_material, physical_state, color, density, stability, solubility,
  storage_condition, intended_use, additional_info, quantity, quantity_unit,
  status, received_at, received_by, recipient_name, received_date,
  inventory_location, created_by, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000031',
  'DEMO-SEED-S-001',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000011',
  'Demo Seed Hydrocolloid Dressing',
  'Demo Seed Hydrocolloid Dressing',
  'STERILE_GAMMA',
  'N/A',
  '1 pc/bag',
  '100 mm',
  'DEMO-SEED-LOT-001',
  'Polymer',
  'Pouch',
  'SOLID',
  'White',
  'N/A',
  'Stable',
  'Insoluble',
  'ROOM_TEMPERATURE',
  'Demo seed sterility validation',
  'Stable demo sample',
  2,
  'box',
  'IN_TESTING',
  '2026-03-10T02:00:00Z',
  '018f0000-0000-7000-8000-000000000094',
  'Demo Seed Sample Manager',
  '2026-03-10',
  'ROOM-A-01',
  '018f0000-0000-7000-8000-000000000094',
  '018f0000-0000-7000-8000-000000000094',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into quotation (
  id, biz_no, project_id, customer_org_id, status, currency, subtotal_amount,
  discount_amount, total_amount, valid_until, note, created_by, updated_by,
  tenant_org_id, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000211',
  'DEMO-SEED-Q-001',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000001',
  'ACCEPTED',
  'CNY',
  1200,
  0,
  1200,
  '2026-04-30',
  'Stable demo quotation',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into quotation_item (
  id, quotation_id, test_name, unit_price, quantity, line_amount, sort_order
) values (
  '018f0000-0000-7000-8000-000000000212',
  '018f0000-0000-7000-8000-000000000211',
  'Sterility',
  1200,
  1,
  1200,
  1
);

insert into contract (
  id, biz_no, project_id, customer_org_id, contract_type, template_code, status,
  signed_at, created_by, updated_by, tenant_org_id, source_type, migration_batch,
  data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000221',
  'DEMO-SEED-C-001',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000001',
  'TEST_AGREEMENT',
  'TEST_AGREEMENT_VER1',
  'SIGNED',
  '2026-03-11T02:00:00Z',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000090',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into study_plan (
  id, biz_no, project_id, status, requires_conditioning, requires_glp,
  requires_animal_test, planned_start_at, planned_finish_at, sd_approved_at,
  created_by, updated_by, tenant_org_id, source_type, migration_batch,
  data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000041',
  'DEMO-SEED-PLAN-001',
  '018f0000-0000-7000-8000-000000000021',
  'DISPATCHED',
  false,
  false,
  false,
  '2026-03-12T01:00:00Z',
  '2026-03-20T09:00:00Z',
  '2026-03-11T08:00:00Z',
  '00000000-0000-0000-0000-000000000106',
  '00000000-0000-0000-0000-000000000106',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into work_order (
  id, biz_no, study_plan_id, project_id, sample_id, owner_user_id, status,
  progress_percent, last_progress_at, planned_finish_at, created_by, updated_by,
  tenant_org_id, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000051',
  'DEMO-SEED-WO-001',
  '018f0000-0000-7000-8000-000000000041',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000031',
  '018f0000-0000-7000-8000-000000000091',
  'COMPLETED',
  100,
  '2026-03-18T09:00:00Z',
  '2026-03-20T09:00:00Z',
  '00000000-0000-0000-0000-000000000106',
  '018f0000-0000-7000-8000-000000000091',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into raw_record (
  id, biz_no, work_order_id, project_id, sample_id, template_code, test_item,
  test_basis, test_time, test_location, temperature_celsius, humidity_percent,
  method, record_data, attachment_file_ids, status, tester_id, checker_id,
  submitted_at, reviewed_at, created_by, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000061',
  'DEMO-SEED-RR-001',
  '018f0000-0000-7000-8000-000000000051',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000031',
  'RAW_RECORD_STERILITY',
  'Sterility',
  'ISO 11737',
  '2026-03-18',
  'Lab A',
  '22',
  '45',
  'DIRECT_INOCULATION',
  '{"conclusion":"No growth observed","sterilityResults":[{"sampleNo":"DEMO-SEED-S-001","conclusion":"STERILE"}]}'::jsonb,
  '[]'::jsonb,
  'APPROVED',
  '018f0000-0000-7000-8000-000000000091',
  '00000000-0000-0000-0000-000000000109',
  '2026-03-18T08:00:00Z',
  '2026-03-18T09:00:00Z',
  '018f0000-0000-7000-8000-000000000091',
  '00000000-0000-0000-0000-000000000109',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into report (
  id, biz_no, project_id, report_template_id, template_code, template_version,
  status, current_version_no, is_official, watermark_type, payment_gate_status,
  issued_at, created_by, updated_by, tenant_org_id, source_type, migration_batch,
  data_quality_score
) values
  (
    '018f0000-0000-7000-8000-000000000071',
    'DEMO-SEED-TR-001',
    '018f0000-0000-7000-8000-000000000021',
    '018f0000-0000-7000-8000-000000000101',
    'STERILITY_REPORT_V1',
    '1.0.0',
    'ISSUED_PAYMENT_LOCKED',
    1,
    true,
    'NONE',
    'LOCKED',
    '2026-04-08T02:20:00Z',
    '018f0000-0000-7000-8000-000000000093',
    '018f0000-0000-7000-8000-000000000093',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  ),
  (
    '018f0000-0000-7000-8000-000000000072',
    'DEMO-SEED-TR-002',
    '018f0000-0000-7000-8000-000000000021',
    '018f0000-0000-7000-8000-000000000101',
    'STERILITY_REPORT_V1',
    '1.0.0',
    'GREEN_RELEASED',
    1,
    true,
    'NONE',
    'GREEN_UNLOCKED',
    '2026-04-08T03:20:00Z',
    '018f0000-0000-7000-8000-000000000093',
    '00000000-0000-0000-0000-000000000115',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  );

insert into report_work_order (report_id, work_order_id, raw_record_id, created_at) values
  ('018f0000-0000-7000-8000-000000000071', '018f0000-0000-7000-8000-000000000051', '018f0000-0000-7000-8000-000000000061', now()),
  ('018f0000-0000-7000-8000-000000000072', '018f0000-0000-7000-8000-000000000051', '018f0000-0000-7000-8000-000000000061', now());

insert into report_version (
  id, report_id, version_no, status, docx_file_id, pdf_file_id, issued_at,
  created_by, updated_by, tenant_org_id, source_type, migration_batch,
  data_quality_score
) values
  (
    '018f0000-0000-7000-8000-000000000111',
    '018f0000-0000-7000-8000-000000000071',
    1,
    'ISSUED',
    '018f0000-0000-7000-8000-000000000112',
    '018f0000-0000-7000-8000-000000000113',
    '2026-04-08T02:20:00Z',
    '018f0000-0000-7000-8000-000000000093',
    '018f0000-0000-7000-8000-000000000093',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  ),
  (
    '018f0000-0000-7000-8000-000000000114',
    '018f0000-0000-7000-8000-000000000072',
    1,
    'ISSUED',
    null,
    '018f0000-0000-7000-8000-000000000115',
    '2026-04-08T03:20:00Z',
    '018f0000-0000-7000-8000-000000000093',
    '00000000-0000-0000-0000-000000000115',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  );

insert into report_delivery_log (
  id, report_id, report_version_id, delivery_type, delivered_to, delivered_by,
  delivered_at, client_type, payment_gate_status, file_id, created_by,
  updated_by, tenant_org_id, source_type, migration_batch, data_quality_score
) values
  (
    '018f0000-0000-7000-8000-000000000161',
    '018f0000-0000-7000-8000-000000000071',
    '018f0000-0000-7000-8000-000000000111',
    'PREVIEW',
    'demo-seed-report@example.com',
    '018f0000-0000-7000-8000-000000000092',
    '2026-04-08T03:00:00Z',
    'PORTAL',
    'LOCKED',
    '018f0000-0000-7000-8000-000000000113',
    '018f0000-0000-7000-8000-000000000092',
    '018f0000-0000-7000-8000-000000000092',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  ),
  (
    '018f0000-0000-7000-8000-000000000162',
    '018f0000-0000-7000-8000-000000000072',
    '018f0000-0000-7000-8000-000000000114',
    'GREEN_CHANNEL_RELEASE',
    'demo-seed-report@example.com',
    '00000000-0000-0000-0000-000000000115',
    '2026-04-08T04:00:00Z',
    'INTERNAL',
    'GREEN_UNLOCKED',
    '018f0000-0000-7000-8000-000000000115',
    '00000000-0000-0000-0000-000000000115',
    '00000000-0000-0000-0000-000000000115',
    '018f0000-0000-7000-8000-000000000001',
    'DEMO_SEED',
    'DEMO_SEED',
    100
  );

insert into green_channel_request (
  id, biz_no, project_id, report_id, reason, status, requested_by, requested_at,
  unlock_scope, created_by, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000231',
  'DEMO-SEED-GC-001',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000072',
  'Demo seed approved electronic report release before payment settlement.',
  'APPROVED',
  '018f0000-0000-7000-8000-000000000092',
  '2026-04-08T03:30:00Z',
  'ELECTRONIC_REPORT_ONLY',
  '018f0000-0000-7000-8000-000000000092',
  '00000000-0000-0000-0000-000000000115',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into change_request (
  id, biz_no, target_type, target_id, target_biz_no, change_type, reason,
  status, requested_by, requested_at, created_by, updated_by, tenant_org_id,
  source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000121',
  'DEMO-SEED-CR-001',
  'REPORT',
  '018f0000-0000-7000-8000-000000000071',
  'DEMO-SEED-TR-001',
  'REPORT_CORRECTION',
  'Demo seed report correction request after issue.',
  'NEEDS_REPORT_REVISION',
  '018f0000-0000-7000-8000-000000000092',
  '2026-04-09T06:10:00Z',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into change_request_item (
  id, change_request_id, field_path, field_label, old_value, new_value,
  lock_policy, sort_order
) values (
  '018f0000-0000-7000-8000-000000000122',
  '018f0000-0000-7000-8000-000000000121',
  'sample.sampleNameEn',
  'Sample English Name',
  'Demo Seed Hydrocolloid Dressing',
  'Sterile Demo Seed Hydrocolloid Dressing',
  'HARD_LOCK',
  1
);

insert into sample_retention (
  id, biz_no, sample_id, project_id, report_id, retained_quantity,
  quantity_unit, storage_condition, storage_location, retained_at, retained_by,
  retain_until, disposition_plan, status, created_by, updated_by, tenant_org_id,
  source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000131',
  'DEMO-SEED-RET-001',
  '018f0000-0000-7000-8000-000000000031',
  '018f0000-0000-7000-8000-000000000021',
  '018f0000-0000-7000-8000-000000000071',
  1,
  'box',
  'ROOM_TEMPERATURE',
  'RET-A-03',
  '2026-03-10T08:00:00Z',
  '018f0000-0000-7000-8000-000000000094',
  '2026-09-10',
  'RETURN',
  'RETAINED',
  '018f0000-0000-7000-8000-000000000094',
  '018f0000-0000-7000-8000-000000000094',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into device (
  id, device_no, name, model, manufacturer, location, owner_user_id,
  metrology_required, status, created_by, updated_by, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000141',
  'DEMO-SEED-DEV-INC-003',
  'Demo Seed Incubator',
  'SMK-37',
  'Demo Seed Instrument',
  'Lab A',
  '018f0000-0000-7000-8000-000000000095',
  true,
  'ACTIVE',
  '018f0000-0000-7000-8000-000000000095',
  '018f0000-0000-7000-8000-000000000095',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into device_booking (
  id, biz_no, device_id, booker_id, booker_type, work_order_id, purpose,
  start_at, end_at, status, created_by, updated_by, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000142',
  'DEMO-SEED-BOOK-001',
  '018f0000-0000-7000-8000-000000000141',
  '018f0000-0000-7000-8000-000000000091',
  'INSPECTOR',
  '018f0000-0000-7000-8000-000000000051',
  'Demo seed sterility incubation',
  '2026-03-18T01:00:00Z',
  '2026-03-18T03:00:00Z',
  'BOOKED',
  '018f0000-0000-7000-8000-000000000091',
  '018f0000-0000-7000-8000-000000000091',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into device_file (
  id, device_id, file_type, file_id, version_no, valid_from, valid_until,
  is_current, uploaded_by, uploaded_at, status, created_by, updated_by,
  source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000151',
  '018f0000-0000-7000-8000-000000000141',
  'CALIBRATION_CERT',
  '018f0000-0000-7000-8000-000000000152',
  '2026',
  '2026-01-01',
  '2026-12-31',
  true,
  '018f0000-0000-7000-8000-000000000095',
  '2026-01-02T01:00:00Z',
  'ACTIVE',
  '018f0000-0000-7000-8000-000000000095',
  '018f0000-0000-7000-8000-000000000095',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into inventory_batch (
  id, batch_no, name, cas_no, specification, manufacturer, received_at,
  expiry_date, quantity, unit, storage_condition, storage_location, status,
  created_by, updated_by, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000181',
  'DEMO-SEED-INV-001',
  'Demo Seed Reference Material',
  '50-00-0',
  '100 mg',
  'Demo Seed Supplier',
  '2026-03-01',
  '2026-12-31',
  7,
  'bottle',
  'ROOM_TEMPERATURE',
  'INV-A-01',
  'IN_STOCK',
  '00000000-0000-0000-0000-000000000118',
  '00000000-0000-0000-0000-000000000118',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into inventory_transaction (
  id, inventory_batch_id, txn_type, quantity, operated_by, operated_at, reason,
  created_by, updated_by, source_type, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000182',
  '018f0000-0000-7000-8000-000000000181',
  'USE',
  3,
  '00000000-0000-0000-0000-000000000118',
  '2026-03-18T01:30:00Z',
  'Demo seed usage',
  '00000000-0000-0000-0000-000000000118',
  '00000000-0000-0000-0000-000000000118',
  'DEMO_SEED',
  100
);

insert into archive_item (
  id, project_id, entity_type, entity_id, archive_type, file_id, status,
  archived_at, archived_by, created_by, updated_by, tenant_org_id, source_type,
  migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000191',
  '018f0000-0000-7000-8000-000000000021',
  'REPORT',
  '018f0000-0000-7000-8000-000000000071',
  'REPORT_OFFICIAL_PDF',
  '018f0000-0000-7000-8000-000000000113',
  'ARCHIVED',
  '2026-04-08T05:00:00Z',
  '00000000-0000-0000-0000-000000000117',
  '00000000-0000-0000-0000-000000000117',
  '00000000-0000-0000-0000-000000000117',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into import_export_task (
  id, biz_no, task_type, entity_type, status, requested_by, requested_at,
  result_file_id, parameters, error_summary, expires_at, created_by, updated_by,
  source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000171',
  'DEMO-SEED-IET-001',
  'REPORT_BATCH_DOWNLOAD',
  'REPORT',
  'SUCCESS',
  '018f0000-0000-7000-8000-000000000096',
  '2026-04-08T07:00:00Z',
  '018f0000-0000-7000-8000-000000000172',
  '{"reportIds":["018f0000-0000-7000-8000-000000000071"],"includeDeliveryLog":true}'::jsonb,
  'SUCCESS: demo seed report batch completed',
  '2026-04-15T07:00:00Z',
  '018f0000-0000-7000-8000-000000000096',
  '018f0000-0000-7000-8000-000000000096',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into work_center_blocker (
  id, project_id, blocker_type, severity, description, impact_scope,
  expected_recovery_at, owner_user_id, collaborator_ids, status, created_by,
  updated_by, tenant_org_id, source_type, migration_batch, data_quality_score
) values (
  '018f0000-0000-7000-8000-000000000201',
  '018f0000-0000-7000-8000-000000000021',
  'PAYMENT_BLOCKED',
  'MEDIUM',
  'Demo seed report is issued but payment is still pending.',
  'Official report download is locked.',
  '2026-04-12T09:00:00Z',
  '018f0000-0000-7000-8000-000000000092',
  '[]'::jsonb,
  'OPEN',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000092',
  '018f0000-0000-7000-8000-000000000001',
  'DEMO_SEED',
  'DEMO_SEED',
  100
);

insert into legacy_status_mapping (
  legacy_entity_type, legacy_status_text, target_entity_type, target_status,
  migration_mode, quality_score_delta, note
) values (
  'DEMO_SEED',
  'DEMO_SEED_REPORT_PENDING',
  'REPORT',
  'DRAFT',
  'MANUAL_REVIEW',
  -10,
  'Stable demo mapping for migration screen validation.'
);

commit;

select 'DEMO_SEED_CUSTOMER_ORG' as entity, count(*) from customer_org where source_type = 'DEMO_SEED'
union all
select 'DEMO_SEED_PROJECT', count(*) from project where source_type = 'DEMO_SEED'
union all
select 'DEMO_SEED_REPORT', count(*) from report where source_type = 'DEMO_SEED'
union all
select 'DEMO_SEED_DEVICE', count(*) from device where source_type = 'DEMO_SEED'
union all
select 'DEMO_SEED_INVENTORY', count(*) from inventory_batch where source_type = 'DEMO_SEED';
SQL

printf 'DEMO SEED COMPLETE\n'
