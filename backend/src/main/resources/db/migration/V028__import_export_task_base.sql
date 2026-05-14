create table import_export_task (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  task_type varchar(64) not null,
  entity_type varchar(64) not null,
  status varchar(32) not null,
  requested_by uuid not null references app_user(id),
  requested_at timestamptz not null,
  source_file_id uuid references file_metadata(id),
  result_file_id uuid references file_metadata(id),
  parameters jsonb not null default '{}'::jsonb,
  error_summary text,
  expires_at timestamptz,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  deleted_at timestamptz,
  version int not null default 0,
  tenant_org_id uuid,
  legacy_id varchar(128),
  source_type varchar(32),
  migration_batch varchar(64),
  data_quality_score int,
  constraint ck_import_export_task_type check (task_type in (
    'MASTER_IMPORT', 'MIGRATION_IMPORT', 'LIST_EXPORT', 'REPORT_BATCH_DOWNLOAD', 'AUDIT_EXPORT'
  )),
  constraint ck_import_export_task_status check (status in (
    'PENDING', 'VALIDATING', 'RUNNING', 'SUCCESS', 'FAILED', 'EXPIRED'
  ))
);

create index ix_import_export_task_type on import_export_task(task_type, requested_at desc);
create index ix_import_export_task_status on import_export_task(status, requested_at desc);
create index ix_import_export_task_requested_by on import_export_task(requested_by, requested_at desc);
