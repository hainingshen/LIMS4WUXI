create table role_definition (
  role_code varchar(64) primary key,
  label_zh varchar(128) not null,
  label_en varchar(128) not null,
  side varchar(32) not null,
  description text not null,
  status varchar(32) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ck_role_definition_side check (side in ('INTERNAL', 'PORTAL')),
  constraint ck_role_definition_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table permission_matrix (
  action_code varchar(64) not null,
  role_code varchar(64) not null references role_definition(role_code),
  access varchar(32) not null,
  scope varchar(64) not null,
  note text,
  primary key (action_code, role_code)
);

create index ix_permission_matrix_role_code on permission_matrix(role_code);

create table dictionary_item (
  dict_code varchar(64) not null,
  item_code varchar(64) not null,
  label_zh varchar(128) not null,
  label_en varchar(128),
  sort_order int not null,
  enabled boolean not null default true,
  primary key (dict_code, item_code)
);

create index ix_dictionary_item_dict_order on dictionary_item(dict_code, sort_order);

create table organization_unit (
  id uuid primary key,
  parent_id uuid references organization_unit(id),
  name varchar(128) not null,
  status varchar(32) not null default 'ACTIVE',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ck_organization_unit_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table app_user (
  id uuid primary key,
  username varchar(128) not null unique,
  display_name varchar(128) not null,
  password_hash varchar(255) not null,
  client_type varchar(32) not null,
  organization_unit_id uuid references organization_unit(id),
  customer_org_id uuid,
  status varchar(32) not null default 'ACTIVE',
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
  constraint ck_app_user_client_type check (client_type in ('INTERNAL', 'PORTAL', 'MOBILE')),
  constraint ck_app_user_status check (status in ('ACTIVE', 'INACTIVE'))
);

create table user_role (
  user_id uuid not null references app_user(id) on delete cascade,
  role_code varchar(64) not null references role_definition(role_code),
  primary key (user_id, role_code)
);

create table system_config (
  id uuid primary key,
  config_key varchar(128) not null unique,
  config_value text not null,
  value_type varchar(16) not null,
  category varchar(64) not null,
  editable_by_role varchar(64) not null,
  description_zh text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ck_system_config_value_type check (value_type in ('INT', 'DECIMAL', 'STRING', 'BOOLEAN', 'JSON'))
);

create table business_rule (
  id uuid primary key,
  rule_code varchar(64) not null unique,
  name_zh varchar(255) not null,
  trigger_type varchar(64) not null,
  condition_expr text not null,
  action_code varchar(64) not null,
  action_payload jsonb,
  priority int not null,
  enabled boolean not null default true,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index ix_business_rule_trigger_enabled on business_rule(trigger_type, enabled, priority);

create table form_schema (
  id uuid primary key,
  form_code varchar(64) not null,
  version_no int not null,
  schema_json jsonb not null,
  ui_schema_json jsonb,
  status varchar(32) not null default 'DRAFT',
  published_at timestamptz,
  published_by uuid,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ux_form_schema_version unique (form_code, version_no),
  constraint ck_form_schema_status check (status in ('DRAFT', 'PUBLISHED', 'DEPRECATED'))
);

create unique index ux_form_schema_published on form_schema(form_code) where status = 'PUBLISHED';

create table approval_record (
  id uuid primary key,
  entity_type varchar(64) not null,
  entity_id uuid not null,
  entity_biz_no varchar(64),
  stage varchar(64) not null,
  outcome varchar(32) not null,
  comment text,
  approver_user_id uuid,
  approver_role_code varchar(64) references role_definition(role_code),
  approved_at timestamptz not null,
  trace_id varchar(64),
  created_at timestamptz not null default now(),
  constraint ck_approval_record_outcome check (outcome in ('APPROVED', 'REJECTED', 'NA'))
);

create index ix_approval_record_entity on approval_record(entity_type, entity_id);
create index ix_approval_record_approver on approval_record(approver_user_id, approved_at);

create table audit_log (
  id uuid primary key,
  occurred_at timestamptz not null,
  actor_user_id uuid,
  actor_role_code varchar(64),
  client_type varchar(32) not null,
  action_code varchar(64) not null,
  entity_type varchar(64),
  entity_id uuid,
  entity_biz_no varchar(64),
  before_snapshot jsonb,
  after_snapshot jsonb,
  result varchar(16) not null,
  failure_reason text,
  ip varchar(64),
  user_agent varchar(512),
  trace_id varchar(64),
  constraint ck_audit_log_client_type check (client_type in ('INTERNAL', 'PORTAL', 'MOBILE', 'SYSTEM')),
  constraint ck_audit_log_result check (result in ('SUCCESS', 'FAILURE'))
);

create index ix_audit_log_occurred_at on audit_log(occurred_at);
create index ix_audit_log_actor on audit_log(actor_user_id, occurred_at);
create index ix_audit_log_entity on audit_log(entity_type, entity_id);
