create table sample_retention (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  sample_id uuid not null references sample(id),
  project_id uuid not null references project(id),
  report_id uuid references report(id),
  retained_quantity decimal(12,3) not null,
  quantity_unit varchar(32) not null,
  storage_condition varchar(64) not null,
  storage_location varchar(255) not null,
  retained_at timestamptz not null,
  retained_by uuid not null references app_user(id),
  retain_until date not null,
  disposition_plan varchar(32) not null,
  status varchar(32) not null,
  proof_file_id uuid references file_metadata(id),
  disposition_reason text,
  disposed_at timestamptz,
  disposed_by uuid references app_user(id),
  extension_reason text,
  extended_at timestamptz,
  extended_by uuid references app_user(id),
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
  constraint ck_sample_retention_disposition_plan check (disposition_plan in ('RETURN', 'DESTROY', 'ARCHIVE', 'EXTEND')),
  constraint ck_sample_retention_status check (status in (
    'PENDING_REGISTER', 'RETAINED', 'EXPIRING', 'PENDING_DISPOSAL', 'RETURNED', 'DESTROYED', 'ARCHIVED'
  ))
);

create index ix_sample_retention_sample on sample_retention(sample_id);
create index ix_sample_retention_project on sample_retention(project_id);
create index ix_sample_retention_status on sample_retention(status);
create index ix_sample_retention_until on sample_retention(retain_until);
