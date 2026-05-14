create table quotation (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  customer_org_id uuid not null,
  status varchar(32) not null,
  currency varchar(8) not null default 'CNY',
  subtotal_amount decimal(14,2) not null,
  discount_amount decimal(14,2) not null default 0,
  total_amount decimal(14,2) not null,
  valid_until date,
  note text,
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
  constraint ck_quotation_status check (status in ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'ISSUED', 'ACCEPTED', 'EXPIRED', 'CANCELLED')),
  constraint ck_quotation_amounts check (subtotal_amount >= 0 and discount_amount >= 0 and total_amount >= 0)
);

create index ix_quotation_project on quotation(project_id);
create index ix_quotation_status on quotation(status);

create table quotation_item (
  id uuid primary key,
  quotation_id uuid not null references quotation(id) on delete cascade,
  test_name varchar(255) not null,
  unit_price decimal(14,2) not null,
  quantity decimal(14,2) not null,
  line_amount decimal(14,2) not null,
  sort_order int not null,
  constraint ck_quotation_item_amounts check (unit_price >= 0 and quantity > 0 and line_amount >= 0)
);

create index ix_quotation_item_quotation on quotation_item(quotation_id, sort_order);
