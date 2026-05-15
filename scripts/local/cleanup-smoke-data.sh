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

"${DOCKER_CMD[@]}" compose exec -T postgres psql \
  -v ON_ERROR_STOP=1 \
  -U "${POSTGRES_USER:-lims}" \
  -d "${POSTGRES_DB:-lims}" <<'SQL'
\set QUIET 1
begin;

create temporary table smoke_submissions as
select id
from submission
where coalesce(data_json::text, '') like '%Local Smoke%'
   or coalesce(official_report_email, '') like 'local-smoke-%@example.com';

create temporary table smoke_projects as
select id
from project
where submission_id in (select id from smoke_submissions)
   or id in (
     select project_id
     from sample
     where coalesce(sample_name_zh, '') like 'Local Smoke%'
   );

create temporary table smoke_samples as
select id
from sample
where project_id in (select id from smoke_projects)
   or coalesce(sample_name_zh, '') like 'Local Smoke%';

create temporary table smoke_study_plans as
select id from study_plan where project_id in (select id from smoke_projects);

create temporary table smoke_work_orders as
select id from work_order where project_id in (select id from smoke_projects)
union
select id from work_order where sample_id in (select id from smoke_samples);

create temporary table smoke_raw_records as
select id from raw_record where project_id in (select id from smoke_projects)
union
select id from raw_record where work_order_id in (select id from smoke_work_orders)
union
select id from raw_record where sample_id in (select id from smoke_samples);

create temporary table smoke_reports as
select id from report where project_id in (select id from smoke_projects);

create temporary table smoke_report_versions as
select id from report_version where report_id in (select id from smoke_reports);

create temporary table smoke_report_templates as
select id
from report_template
where coalesce(template_version, '') like 'SMOKE-%'
   or coalesce(template_name, '') like 'Local Smoke%'
   or coalesce(legacy_template_name, '') like 'local-smoke%';

create temporary table smoke_devices as
select id from device where coalesce(device_no, '') like 'DEV-SMOKE-%';

create temporary table smoke_inventory_batches as
select id from inventory_batch where coalesce(batch_no, '') like 'INV-SMOKE-%';

create temporary table smoke_files as
select file_id as id from sample_photo where sample_id in (select id from smoke_samples)
union
select file_id from archive_item where project_id in (select id from smoke_projects)
union
select voucher_file_id from payment_receipt where project_id in (select id from smoke_projects) and voucher_file_id is not null
union
select source_file_id from import_export_task where source_file_id is not null and parameters::text like '%REPORT_BATCH_DOWNLOAD%'
union
select result_file_id from import_export_task where result_file_id is not null and parameters::text like '%REPORT_BATCH_DOWNLOAD%'
union
select file_id from device_file where device_id in (select id from smoke_devices)
union
select file_id from report_template where id in (select id from smoke_report_templates)
union
select docx_file_id from report_version where report_id in (select id from smoke_reports) and docx_file_id is not null
union
select pdf_file_id from report_version where report_id in (select id from smoke_reports) and pdf_file_id is not null
union
select data_snapshot_file_id from report_version where report_id in (select id from smoke_reports) and data_snapshot_file_id is not null
union
select id from file_metadata where coalesce(file_name, '') like 'local-smoke-%'
union
select id from file_metadata where coalesce(object_key, '') like 'samples/%/photos/%local-smoke%'
union
select id from file_metadata where coalesce(object_key, '') like 'devices/%/%' and coalesce(file_name, '') like 'local-smoke-%';

create temporary table smoke_users as
select id
from app_user
where username like 'portal.smoke.%'
   or username like 'internal.smoke.%'
   or username like 'internal.ethics.smoke.%';

with deleted as (
  delete from report_delivery_log
  where report_id in (select id from smoke_reports)
     or report_version_id in (select id from smoke_report_versions)
  returning 1
)
select 'report_delivery_log', count(*) from deleted;

with deleted as (
  delete from import_export_task
  where parameters::text like '%REPORT_BATCH_DOWNLOAD%'
    and (
      parameters::text like any (array(select '%' || id::text || '%' from smoke_reports))
      or result_file_id in (select id from smoke_files)
      or source_file_id in (select id from smoke_files)
    )
  returning 1
)
select 'import_export_task', count(*) from deleted;

with deleted as (
  delete from archive_item where project_id in (select id from smoke_projects) returning 1
)
select 'archive_item', count(*) from deleted;

with deleted as (
  delete from sample_retention
  where project_id in (select id from smoke_projects)
     or sample_id in (select id from smoke_samples)
     or report_id in (select id from smoke_reports)
  returning 1
)
select 'sample_retention', count(*) from deleted;

with deleted as (
  delete from payment_receipt
  where project_id in (select id from smoke_projects)
     or report_id in (select id from smoke_reports)
  returning 1
)
select 'payment_receipt', count(*) from deleted;

with deleted as (
  delete from green_channel_request
  where project_id in (select id from smoke_projects)
     or report_id in (select id from smoke_reports)
  returning 1
)
select 'green_channel_request', count(*) from deleted;

with deleted as (
  delete from report_work_order
  where report_id in (select id from smoke_reports)
     or work_order_id in (select id from smoke_work_orders)
     or raw_record_id in (select id from smoke_raw_records)
  returning 1
)
select 'report_work_order', count(*) from deleted;

with deleted as (
  delete from report_version where report_id in (select id from smoke_reports) returning 1
)
select 'report_version', count(*) from deleted;

with deleted as (
  delete from report where id in (select id from smoke_reports) returning 1
)
select 'report', count(*) from deleted;

with deleted as (
  delete from device_booking
  where device_id in (select id from smoke_devices)
     or work_order_id in (select id from smoke_work_orders)
  returning 1
)
select 'device_booking', count(*) from deleted;

with deleted as (
  delete from equipment_usage
  where raw_record_id in (select id from smoke_raw_records)
     or device_id in (select id from smoke_devices)
     or device_no like 'EQ-SMOKE%'
  returning 1
)
select 'equipment_usage', count(*) from deleted;

with deleted as (
  delete from raw_record where id in (select id from smoke_raw_records) returning 1
)
select 'raw_record', count(*) from deleted;

with deleted as (
  delete from work_order where id in (select id from smoke_work_orders) returning 1
)
select 'work_order', count(*) from deleted;

with deleted as (
  delete from study_plan where id in (select id from smoke_study_plans) returning 1
)
select 'study_plan', count(*) from deleted;

with deleted as (
  delete from sample_photo where sample_id in (select id from smoke_samples) returning 1
)
select 'sample_photo', count(*) from deleted;

with deleted as (
  delete from sample where id in (select id from smoke_samples) returning 1
)
select 'sample', count(*) from deleted;

with deleted as (
  delete from contract where project_id in (select id from smoke_projects) returning 1
)
select 'contract', count(*) from deleted;

with deleted as (
  delete from quotation_item
  where quotation_id in (select id from quotation where project_id in (select id from smoke_projects))
  returning 1
)
select 'quotation_item', count(*) from deleted;

with deleted as (
  delete from quotation where project_id in (select id from smoke_projects) returning 1
)
select 'quotation', count(*) from deleted;

with deleted as (
  delete from work_center_blocker where project_id in (select id from smoke_projects) returning 1
)
select 'work_center_blocker', count(*) from deleted;

with deleted as (
  delete from project where id in (select id from smoke_projects) returning 1
)
select 'project', count(*) from deleted;

with deleted as (
  delete from submission_test_selection where submission_id in (select id from smoke_submissions) returning 1
)
select 'submission_test_selection', count(*) from deleted;

with deleted as (
  delete from submission where id in (select id from smoke_submissions) returning 1
)
select 'submission', count(*) from deleted;

with deleted as (
  delete from device_file where device_id in (select id from smoke_devices) returning 1
)
select 'device_file', count(*) from deleted;

with deleted as (
  delete from device where id in (select id from smoke_devices) returning 1
)
select 'device', count(*) from deleted;

with deleted as (
  delete from inventory_transaction where inventory_batch_id in (select id from smoke_inventory_batches) returning 1
)
select 'inventory_transaction', count(*) from deleted;

with deleted as (
  delete from inventory_batch where id in (select id from smoke_inventory_batches) returning 1
)
select 'inventory_batch', count(*) from deleted;

with deleted as (
  delete from report_template where id in (select id from smoke_report_templates) returning 1
)
select 'report_template', count(*) from deleted;

with deleted as (
  delete from user_role where user_id in (select id from smoke_users) returning 1
)
select 'user_role', count(*) from deleted;

with deleted as (
  delete from app_user where id in (select id from smoke_users) returning 1
)
select 'app_user', count(*) from deleted;

with deleted as (
  delete from customer_contact
  where email like 'local-smoke-%@example.com'
     or contact_name = 'Local Smoke Contact'
  returning 1
)
select 'customer_contact', count(*) from deleted;

with deleted as (
  delete from manufacturer
  where name_zh = 'Local Smoke Manufacturer'
     or name_en = 'Local Smoke Manufacturer'
  returning 1
)
select 'manufacturer', count(*) from deleted;

with deleted as (
  delete from customer_org
  where name_zh like 'Local Smoke Customer %'
     or name_en like 'Local Smoke Customer %'
  returning 1
)
select 'customer_org', count(*) from deleted;

with deleted as (
  delete from system_config
  where config_key like 'LOCAL_SMOKE_CONFIG_%'
     or category = 'LOCAL_SMOKE'
  returning 1
)
select 'system_config', count(*) from deleted;

with deleted as (
  delete from business_rule
  where rule_code like 'LOCAL_SMOKE_RULE_%'
     or trigger_type = 'LOCAL_SMOKE'
     or action_code = 'LOCAL_SMOKE_ACTION'
  returning 1
)
select 'business_rule', count(*) from deleted;

with deleted as (
  delete from holiday where name = 'Local Smoke Holiday' returning 1
)
select 'holiday', count(*) from deleted;

with deleted as (
  delete from legacy_status_mapping where legacy_status_text like 'LOCAL_SMOKE_STATUS_%' returning 1
)
select 'legacy_status_mapping', count(*) from deleted;

with deleted as (
  delete from audit_log
  where coalesce(entity_biz_no, '') like 'LOCAL_SMOKE%'
     or coalesce(failure_reason, '') like '%Local Smoke%'
     or coalesce(before_snapshot::text, '') like '%Local Smoke%'
     or coalesce(after_snapshot::text, '') like '%Local Smoke%'
     or entity_id in (
       select id from smoke_submissions
       union select id from smoke_projects
       union select id from smoke_samples
       union select id from smoke_reports
       union select id from smoke_users
     )
  returning 1
)
select 'audit_log', count(*) from deleted;

with deleted as (
  delete from file_metadata
  where id in (select id from smoke_files)
     or file_name like 'local-smoke-%'
  returning 1
)
select 'file_metadata', count(*) from deleted;

commit;
SQL
