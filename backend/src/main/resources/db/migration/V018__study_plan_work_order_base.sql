insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000106',
  'internal.sd',
  'Internal Study Director',
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
  ('00000000-0000-0000-0000-000000000106', 'SD')
on conflict (user_id, role_code) do nothing;

create table study_plan (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  status varchar(32) not null,
  requires_conditioning boolean not null,
  requires_glp boolean not null,
  requires_animal_test boolean not null,
  planned_start_at timestamptz,
  planned_finish_at timestamptz,
  sd_approved_at timestamptz,
  qa_approved_at timestamptz,
  ethics_approved_at timestamptz,
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
  constraint ck_study_plan_status check (status in (
    'DRAFT', 'REVIEWING', 'PENDING_DISPATCH', 'READY_TO_DISPATCH', 'DISPATCHED', 'COMPLETED'
  ))
);

create index ix_study_plan_project on study_plan(project_id);
create index ix_study_plan_status on study_plan(status);

create table work_order (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  study_plan_id uuid not null references study_plan(id),
  project_id uuid not null references project(id),
  sample_id uuid not null references sample(id),
  test_item_id uuid,
  test_sub_item_id uuid,
  owner_user_id uuid not null references app_user(id),
  status varchar(32) not null,
  progress_percent int not null,
  last_progress_at timestamptz,
  planned_finish_at timestamptz,
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
  constraint ck_work_order_status check (status in (
    'PENDING_CONFIRM', 'PENDING_ASSIGN', 'PENDING_EXECUTION', 'SAMPLE_PICKED',
    'IN_PROGRESS', 'PAUSED', 'RESULT_ENTERED', 'RAW_REVIEWING', 'COMPLETED'
  )),
  constraint ck_work_order_progress check (progress_percent between 0 and 100)
);

create index ix_work_order_plan on work_order(study_plan_id);
create index ix_work_order_project on work_order(project_id);
create index ix_work_order_sample on work_order(sample_id);
create index ix_work_order_status on work_order(status);
