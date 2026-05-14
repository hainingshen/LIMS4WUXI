insert into permission_matrix (action_code, role_code, access, scope, note) values
  ('WORK_CENTER_BLOCKER_MANAGE', 'QUALITY_MANAGER', 'ALLOW', 'ALL', '质量负责人维护工作中心阻塞事项')
on conflict (action_code, role_code) do update set
  access = excluded.access,
  scope = excluded.scope,
  note = excluded.note;

insert into system_config (
  id, config_key, config_value, value_type, category, editable_by_role, description_zh
) values
  ('00000000-0000-0000-0000-000000000c86', 'WORK_CENTER_BLOCKER_PENALTY', '10', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '单个未关闭阻塞扣分'),
  ('00000000-0000-0000-0000-000000000c87', 'WORK_CENTER_BLOCKER_MAX_PENALTY', '30', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '未关闭阻塞最大扣分')
on conflict (config_key) do update set
  config_value = excluded.config_value,
  value_type = excluded.value_type,
  category = excluded.category,
  editable_by_role = excluded.editable_by_role,
  description_zh = excluded.description_zh,
  updated_at = now();

create table work_center_blocker (
  id uuid primary key,
  project_id uuid not null references project(id),
  blocker_type varchar(64) not null,
  severity varchar(16) not null,
  description text not null,
  impact_scope text not null,
  expected_recovery_at timestamptz not null,
  owner_user_id uuid not null references app_user(id),
  collaborator_ids jsonb not null default '[]'::jsonb,
  status varchar(32) not null,
  resolution text,
  closed_at timestamptz,
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
  constraint ck_work_center_blocker_type check (blocker_type in (
    'PENDING_APPROVAL', 'MISSING_SAMPLE', 'MISSING_MATERIAL', 'DEVICE_CONFLICT',
    'STAFF_UNAVAILABLE', 'TECHNICAL_RISK', 'CUSTOMER_CONFIRMATION',
    'SUBCONTRACT_DELAY', 'PAYMENT_BLOCKED', 'REPORT_REWORK', 'OTHER'
  )),
  constraint ck_work_center_blocker_severity check (severity in ('LOW', 'MEDIUM', 'HIGH')),
  constraint ck_work_center_blocker_status check (status in ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
  constraint ck_work_center_blocker_collaborators check (jsonb_typeof(collaborator_ids) = 'array'),
  constraint ck_work_center_blocker_resolution check (status <> 'CLOSED' or nullif(btrim(resolution), '') is not null)
);

create index ix_work_center_blocker_project on work_center_blocker(project_id, status);
create index ix_work_center_blocker_owner on work_center_blocker(owner_user_id, status);
create index ix_work_center_blocker_recovery on work_center_blocker(expected_recovery_at) where status <> 'CLOSED';
