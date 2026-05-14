create table contract (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null references project(id),
  customer_org_id uuid not null references customer_org(id),
  contract_type varchar(64) not null,
  template_code varchar(128) not null,
  status varchar(32) not null,
  source_file_id uuid,
  signed_at timestamptz,
  signed_file_id uuid,
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
  constraint ck_contract_type check (contract_type in ('TEST_AGREEMENT', 'TECHNICAL_COOPERATION', 'SUPPLEMENTAL', 'SUBCONTRACT')),
  constraint ck_contract_status check (status in ('DRAFT', 'PENDING_SIGN', 'SIGNED', 'CANCELLED', 'SUPERSEDED'))
);

create index ix_contract_project on contract(project_id);
create index ix_contract_customer_org on contract(customer_org_id);
create index ix_contract_status on contract(status);
