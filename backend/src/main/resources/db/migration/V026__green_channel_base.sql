insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values
  (
    '00000000-0000-0000-0000-000000000114',
    'internal.dept_manager',
    'Internal Department Manager',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    null,
    'ACTIVE'
  ),
  (
    '00000000-0000-0000-0000-000000000115',
    'internal.general_manager',
    'Internal General Manager',
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
  ('00000000-0000-0000-0000-000000000114', 'DEPT_MANAGER'),
  ('00000000-0000-0000-0000-000000000115', 'GENERAL_MANAGER')
on conflict (user_id, role_code) do nothing;

create table green_channel_request (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  report_id uuid not null references report(id),
  reason text not null,
  status varchar(32) not null,
  requested_by uuid not null references app_user(id),
  requested_at timestamptz not null,
  unlock_scope varchar(64) not null,
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
  constraint ck_green_channel_status check (status in (
    'DRAFT', 'DEPT_REVIEWING', 'MANAGEMENT_REVIEWING', 'APPROVED', 'REJECTED', 'UNLOCKED', 'CLOSED'
  )),
  constraint ck_green_channel_unlock_scope check (unlock_scope in ('ELECTRONIC_REPORT_ONLY'))
);

create index ix_green_channel_project on green_channel_request(project_id, requested_at desc);
create index ix_green_channel_report on green_channel_request(report_id);
create index ix_green_channel_status on green_channel_request(status);
