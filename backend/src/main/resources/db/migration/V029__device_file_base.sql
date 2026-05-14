insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000116',
  'internal.equipment_manager',
  'Internal Equipment Manager',
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
  ('00000000-0000-0000-0000-000000000116', 'EQUIPMENT_MANAGER')
on conflict (user_id, role_code) do nothing;

create table device (
  id uuid primary key,
  device_no varchar(128) not null unique,
  name varchar(255) not null,
  model varchar(128),
  manufacturer varchar(255),
  location varchar(255) not null,
  owner_user_id uuid references app_user(id),
  metrology_required boolean not null default false,
  status varchar(32) not null,
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
  constraint ck_device_status check (status in ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'DISABLED'))
);

create index ix_device_status on device(status);
create index ix_device_name on device(name);

create table device_file (
  id uuid primary key,
  device_id uuid not null references device(id),
  file_type varchar(64) not null,
  file_id uuid not null references file_metadata(id),
  version_no varchar(32),
  valid_from date,
  valid_until date,
  is_current boolean not null,
  uploaded_by uuid not null references app_user(id),
  uploaded_at timestamptz not null,
  status varchar(32) not null,
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
  constraint ck_device_file_type check (file_type in (
    'MANUAL', 'CALIBRATION_CERT', 'VERIFICATION_CERT', 'MAINTENANCE_RECORD',
    'SOP', 'TRAINING_RECORD', 'OTHER'
  )),
  constraint ck_device_file_status check (status in ('ACTIVE', 'DISABLED'))
);

create unique index ux_device_file_current
  on device_file(device_id, file_type)
  where is_current = true and status = 'ACTIVE' and deleted_at is null;

create index ix_device_file_device on device_file(device_id, file_type);
create index ix_device_file_valid_until on device_file(valid_until) where status = 'ACTIVE';

create table equipment_usage (
  id uuid primary key,
  raw_record_id uuid not null references raw_record(id) on delete cascade,
  device_id uuid references device(id),
  device_name varchar(255) not null,
  device_no varchar(128) not null,
  model varchar(128),
  calibration_valid_until date,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  deleted_at timestamptz,
  version int not null default 0,
  source_type varchar(32),
  data_quality_score int
);

create index ix_equipment_usage_raw_record on equipment_usage(raw_record_id);
create index ix_equipment_usage_device on equipment_usage(device_id);
