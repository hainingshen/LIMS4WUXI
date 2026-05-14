insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000113',
  'internal.finance',
  'Internal Finance Cashier',
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
  ('00000000-0000-0000-0000-000000000113', 'FINANCE_CASHIER')
on conflict (user_id, role_code) do nothing;

create table payment_receipt (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  report_id uuid references report(id),
  amount numeric(14, 2) not null,
  received_at timestamptz not null,
  received_by uuid not null references app_user(id),
  receipt_method varchar(32) not null,
  voucher_file_id uuid references file_metadata(id),
  note text,
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
  constraint ck_payment_receipt_amount check (amount > 0),
  constraint ck_payment_receipt_method check (receipt_method in ('BANK_TRANSFER', 'CASH', 'OTHER')),
  constraint ck_payment_receipt_status check (status in ('RECORDED', 'REVERSED'))
);

create index ix_payment_receipt_project on payment_receipt(project_id, received_at desc);
create index ix_payment_receipt_report on payment_receipt(report_id);
create index ix_payment_receipt_status on payment_receipt(status);
