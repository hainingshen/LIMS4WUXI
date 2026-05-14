insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000109',
  'internal.qc',
  'Internal QC',
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
  ('00000000-0000-0000-0000-000000000109', 'QC')
on conflict (user_id, role_code) do nothing;

create table raw_record (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  work_order_id uuid not null references work_order(id),
  project_id uuid not null references project(id),
  sample_id uuid not null references sample(id),
  template_code varchar(64) not null,
  test_item varchar(128) not null,
  test_basis text not null,
  test_time varchar(128),
  test_location varchar(255),
  temperature_celsius varchar(64),
  humidity_percent varchar(64),
  method varchar(64),
  record_data jsonb not null,
  attachment_file_ids jsonb not null default '[]'::jsonb,
  status varchar(32) not null,
  tester_id uuid not null references app_user(id),
  checker_id uuid references app_user(id),
  submitted_at timestamptz not null,
  reviewed_at timestamptz,
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
  constraint ck_raw_record_status check (status in (
    'DRAFT', 'SUBMITTED', 'REVIEWING', 'APPROVED', 'REJECTED'
  ))
);

create index ix_raw_record_work_order on raw_record(work_order_id);
create index ix_raw_record_project on raw_record(project_id);
create index ix_raw_record_sample on raw_record(sample_id);
create index ix_raw_record_status on raw_record(status);
