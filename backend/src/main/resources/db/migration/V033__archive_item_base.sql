insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000117',
  'internal.archive',
  'Internal Archive Manager',
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
  ('00000000-0000-0000-0000-000000000117', 'ARCHIVE_MANAGER')
on conflict (user_id, role_code) do nothing;

create table archive_item (
  id uuid primary key,
  project_id uuid not null references project(id),
  entity_type varchar(64),
  entity_id uuid,
  archive_type varchar(64) not null,
  file_id uuid not null references file_metadata(id),
  status varchar(32) not null,
  archived_at timestamptz not null,
  archived_by uuid references app_user(id),
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
  constraint ck_archive_item_status check (status in ('ARCHIVED', 'SUPERSEDED', 'VOIDED'))
);

create index ix_archive_item_project on archive_item(project_id, archived_at desc);
create index ix_archive_item_entity on archive_item(entity_type, entity_id);
create index ix_archive_item_file on archive_item(file_id);
