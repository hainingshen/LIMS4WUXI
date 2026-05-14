insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000110',
  'internal.report_preparer',
  'Internal Report Preparer',
  '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
  'INTERNAL',
  null,
  'ACTIVE'
)
on conflict (username) do update set
  display_name = excluded.display_name,
  password_hash = excluded.password_hash,
  client_type = excluded.client_type,
  customer_org_id = excluded.customer_org_id,
  status = excluded.status;

insert into user_role (user_id, role_code) values
  ('00000000-0000-0000-0000-000000000110', 'REPORT_PREPARER')
on conflict (user_id, role_code) do nothing;

create table report_version (
  id uuid primary key,
  report_id uuid not null references report(id) on delete cascade,
  version_no int not null,
  status varchar(32) not null,
  docx_file_id uuid references file_metadata(id),
  pdf_file_id uuid references file_metadata(id),
  data_snapshot_file_id uuid references file_metadata(id),
  render_log text,
  revision_reason text,
  issued_at timestamptz,
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
  constraint ux_report_version_no unique (report_id, version_no),
  constraint ck_report_version_status check (status in ('DRAFT', 'ISSUED', 'VOIDED', 'SUPERSEDED'))
);

create index ix_report_version_report on report_version(report_id, version_no);
create index ix_report_version_status on report_version(status);
