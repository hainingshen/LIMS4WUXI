insert into permission_matrix (action_code, role_code, access, scope, note) values
  ('INVENTORY_READ', 'INVENTORY_MANAGER', 'READ', 'ALL', '标准品管理员查看库存台账'),
  ('INVENTORY_READ', 'QUALITY_MANAGER', 'READ', 'ALL', '质量负责人查看库存预警和台账')
on conflict (action_code, role_code) do update set
  access = excluded.access,
  scope = excluded.scope,
  note = excluded.note;

insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000118',
  'internal.inventory',
  'Internal Inventory Manager',
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
  ('00000000-0000-0000-0000-000000000118', 'INVENTORY_MANAGER')
on conflict (user_id, role_code) do nothing;

create table inventory_batch (
  id uuid primary key,
  batch_no varchar(128) not null unique,
  name varchar(255) not null,
  cas_no varchar(128),
  specification varchar(255),
  manufacturer varchar(255),
  received_at date,
  expiry_date date,
  quantity numeric(18, 3) not null,
  unit varchar(64) not null,
  storage_condition varchar(64) not null,
  storage_location varchar(255),
  opened_at date,
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
  constraint ck_inventory_batch_quantity_nonnegative check (quantity >= 0),
  constraint ck_inventory_batch_status check (status in (
    'PENDING_ACCEPTANCE', 'IN_STOCK', 'OPENED', 'LOW_STOCK', 'EXPIRED',
    'DEPLETED', 'SCRAPPED', 'ARCHIVED', 'REJECTED'
  ))
);

create index ix_inventory_batch_name on inventory_batch(name);
create index ix_inventory_batch_expiry on inventory_batch(expiry_date);
create index ix_inventory_batch_status on inventory_batch(status);

create table inventory_transaction (
  id uuid primary key,
  inventory_batch_id uuid not null references inventory_batch(id),
  txn_type varchar(32) not null,
  quantity numeric(18, 3) not null,
  operated_by uuid not null references app_user(id),
  operated_at timestamptz not null,
  reason text,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  deleted_at timestamptz,
  version int not null default 0,
  source_type varchar(32),
  data_quality_score int,
  constraint ck_inventory_txn_quantity_positive check (quantity > 0),
  constraint ck_inventory_txn_type check (txn_type in ('IN', 'OUT', 'USE', 'RETURN', 'SCRAP', 'ADJUST'))
);

create index ix_inventory_txn_batch on inventory_transaction(inventory_batch_id, operated_at desc);
