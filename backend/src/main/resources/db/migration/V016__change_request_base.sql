create table report (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  report_template_id uuid,
  template_code varchar(64) not null,
  template_version varchar(32) not null,
  status varchar(32) not null,
  current_version_no int not null default 1,
  is_official boolean not null default false,
  watermark_type varchar(32) not null default 'DRAFT',
  payment_gate_status varchar(32) not null default 'LOCKED',
  issued_at timestamptz,
  released_at timestamptz,
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
  constraint ck_report_status check (status in (
    'PENDING_GENERATE', 'DRAFT', 'QC_REVIEWING', 'QA_REVIEWING', 'SD_REVIEWING',
    'PENDING_ISSUE', 'ISSUED_PAYMENT_LOCKED', 'GREEN_RELEASED', 'RELEASED',
    'REVISION_DRAFT', 'ARCHIVED', 'VOIDED'
  )),
  constraint ck_report_watermark_type check (watermark_type in ('DRAFT', 'TEST', 'NONE')),
  constraint ck_report_payment_gate_status check (payment_gate_status in ('LOCKED', 'UNLOCKED', 'GREEN_UNLOCKED'))
);

create index ix_report_project on report(project_id);
create index ix_report_status on report(status);

create table change_request (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  target_type varchar(64) not null,
  target_id uuid not null,
  target_biz_no varchar(64),
  change_type varchar(64) not null,
  reason text not null,
  status varchar(32) not null,
  requested_by uuid not null references app_user(id),
  requested_at timestamptz not null,
  effective_at timestamptz,
  attachment_file_id uuid references file_metadata(id),
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
  constraint ck_change_request_target_type check (target_type in ('SUBMISSION', 'CONTRACT', 'SAMPLE', 'PROJECT', 'REPORT')),
  constraint ck_change_request_change_type check (change_type in ('FIELD_CORRECTION', 'SCOPE_CHANGE', 'COST_PERIOD_CHANGE', 'REPORT_CORRECTION', 'OTHER')),
  constraint ck_change_request_status check (status in ('DRAFT', 'SUBMITTED', 'REVIEWING', 'REJECTED', 'APPROVED', 'NEEDS_REPORT_REVISION', 'EFFECTIVE', 'ARCHIVED', 'CANCELLED'))
);

create index ix_change_request_target on change_request(target_type, target_id);
create index ix_change_request_status on change_request(status);
create index ix_change_request_requested_by on change_request(requested_by);

create table change_request_item (
  id uuid primary key,
  change_request_id uuid not null references change_request(id) on delete cascade,
  field_path varchar(255) not null,
  field_label varchar(255) not null,
  old_value text,
  new_value text,
  lock_policy varchar(32) not null,
  sort_order int not null default 0,
  constraint ck_change_request_item_lock_policy check (lock_policy in ('FREELY_EDITABLE', 'CHANGE_REQUEST', 'HARD_LOCK'))
);

create index ix_change_request_item_request on change_request_item(change_request_id, sort_order);
