create table customer_org (
  id uuid primary key,
  biz_no varchar(64) unique,
  name_zh varchar(255) not null,
  name_en varchar(255),
  address_zh text not null,
  address_en text,
  industry varchar(128),
  grade varchar(64),
  tax_no varchar(64),
  invoice_title varchar(255),
  bank_account varchar(255),
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
  constraint ck_customer_org_status check (status in ('ACTIVE', 'INACTIVE'))
);

create index ix_customer_org_name_zh on customer_org(name_zh);
create index ix_customer_org_status on customer_org(status);

create table customer_contact (
  id uuid primary key,
  biz_no varchar(64) unique,
  customer_org_id uuid not null references customer_org(id),
  contact_name varchar(128) not null,
  phone varchar(64) not null,
  email varchar(255) not null,
  is_primary boolean not null default false,
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
  constraint ck_customer_contact_status check (status in ('ACTIVE', 'INACTIVE'))
);

create index ix_customer_contact_org on customer_contact(customer_org_id);
create index ix_customer_contact_email on customer_contact(email);

create table manufacturer (
  id uuid primary key,
  biz_no varchar(64) unique,
  name_zh varchar(255) not null,
  name_en varchar(255),
  address_zh text not null,
  address_en text,
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
  constraint ck_manufacturer_status check (status in ('ACTIVE', 'INACTIVE'))
);

create index ix_manufacturer_name_zh on manufacturer(name_zh);
create index ix_manufacturer_status on manufacturer(status);

insert into customer_org (
  id,
  biz_no,
  name_zh,
  name_en,
  address_zh,
  address_en,
  status,
  tenant_org_id,
  source_type,
  data_quality_score
) values (
  '00000000-0000-0000-0000-000000000301',
  'CUST-DEMO-301',
  'Demo Sponsor',
  'Demo Sponsor',
  'No.1 Demo Road, Wuxi',
  'No.1 Demo Road, Wuxi',
  'ACTIVE',
  '00000000-0000-0000-0000-000000000301',
  'NEW',
  100
);

insert into customer_contact (
  id,
  biz_no,
  customer_org_id,
  contact_name,
  phone,
  email,
  is_primary,
  status,
  tenant_org_id,
  source_type,
  data_quality_score
) values (
  '00000000-0000-0000-0000-000000000302',
  'CONTACT-DEMO-302',
  '00000000-0000-0000-0000-000000000301',
  'Demo Applicant',
  '13800000000',
  'applicant@example.com',
  true,
  'ACTIVE',
  '00000000-0000-0000-0000-000000000301',
  'NEW',
  100
);

insert into manufacturer (
  id,
  biz_no,
  name_zh,
  name_en,
  address_zh,
  address_en,
  status,
  source_type,
  data_quality_score
) values (
  '00000000-0000-0000-0000-000000000303',
  'MFR-DEMO-303',
  'Demo Manufacturer',
  'Demo Manufacturer',
  'No.1 Manufacturing Road, Wuxi',
  'No.1 Manufacturing Road, Wuxi',
  'ACTIVE',
  'NEW',
  100
);

alter table app_user
  add constraint fk_app_user_customer_org foreign key (customer_org_id) references customer_org(id);

alter table submission
  add column customer_contact_id uuid,
  add column manufacturer_id uuid,
  add column official_report_email varchar(512),
  add column shipping_info text,
  add column requires_glp boolean,
  add column requires_hard_copy_of_electronic_report boolean,
  add column report_language varchar(16),
  add column protocol_language varchar(16),
  add column submitted_quantity_text varchar(255),
  add column sample_disposition varchar(32),
  add column sample_disposition_remark text,
  add column change_locked_at timestamptz,
  add column glp_characterization_affirmed boolean,
  add column glp_characterization_statement_version varchar(64),
  add column glp_characterization_confirmed_by varchar(128),
  add column glp_characterization_confirmed_at timestamptz,
  add column tenant_org_id uuid,
  add column legacy_id varchar(128),
  add column source_type varchar(32),
  add column migration_batch varchar(64),
  add column data_quality_score int;

update submission
set customer_contact_id = '00000000-0000-0000-0000-000000000302',
    manufacturer_id = '00000000-0000-0000-0000-000000000303',
    official_report_email = 'reports@example.com',
    shipping_info = 'Demo shipping information',
    requires_glp = false,
    requires_hard_copy_of_electronic_report = false,
    report_language = 'CHS',
    protocol_language = 'CHS',
    submitted_quantity_text = 'N/A',
    sample_disposition = 'DESTROY_DISCARD',
    tenant_org_id = customer_org_id,
    source_type = 'NEW',
    data_quality_score = 100
where customer_contact_id is null;

alter table submission
  alter column customer_contact_id set not null,
  alter column manufacturer_id set not null,
  alter column official_report_email set not null,
  alter column shipping_info set not null,
  alter column requires_glp set not null,
  alter column requires_hard_copy_of_electronic_report set not null,
  alter column report_language set not null,
  alter column protocol_language set not null,
  alter column submitted_quantity_text set not null,
  alter column sample_disposition set not null;

alter table submission
  add constraint fk_submission_customer_org foreign key (customer_org_id) references customer_org(id),
  add constraint fk_submission_customer_contact foreign key (customer_contact_id) references customer_contact(id),
  add constraint fk_submission_manufacturer foreign key (manufacturer_id) references manufacturer(id),
  add constraint ck_submission_report_language check (report_language in ('CHS', 'ENG', 'BOTH')),
  add constraint ck_submission_protocol_language check (protocol_language in ('CHS', 'ENG', 'BOTH')),
  add constraint ck_submission_sample_disposition check (sample_disposition in ('DESTROY_DISCARD', 'RETURN_UNUSED', 'OTHER'));

create index ix_submission_customer_contact on submission(customer_contact_id);
create index ix_submission_manufacturer on submission(manufacturer_id);

create table submission_test_selection (
  id uuid primary key,
  biz_no varchar(64) unique,
  submission_id uuid not null references submission(id) on delete cascade,
  category varchar(32) not null,
  test_code varchar(64) not null,
  test_name varchar(255) not null,
  method_name varchar(255),
  extractant varchar(255),
  standard varchar(255),
  instruction text,
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
  constraint ck_submission_test_selection_category check (category in ('BIOCOMPATIBILITY', 'MICROBIOLOGY', 'PACKAGING', 'PHYSICAL', 'CHEMICAL', 'OTHER'))
);

create index ix_submission_test_selection_submission on submission_test_selection(submission_id);
create index ix_submission_test_selection_test_code on submission_test_selection(test_code);

create table sample (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  project_id uuid not null,
  submission_id uuid not null references submission(id),
  sample_name_zh varchar(255) not null,
  sample_name_en varchar(255) not null,
  sterilization_state varchar(32) not null,
  cas_code varchar(128) not null,
  model_ref varchar(255) not null,
  size_spec varchar(255) not null,
  lot_batch varchar(128) not null,
  raw_material text not null,
  packaging_material text not null,
  physical_state varchar(64) not null,
  color varchar(128) not null,
  density varchar(128) not null,
  stability text not null,
  solubility text not null,
  storage_condition varchar(64) not null,
  intended_use text not null,
  additional_info text not null,
  quantity decimal(12,3) not null,
  quantity_unit varchar(32) not null,
  status varchar(32) not null,
  received_at timestamptz,
  received_by uuid references app_user(id),
  recipient_name varchar(128),
  received_date date,
  inventory_location varchar(128),
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
  constraint ck_sample_sterilization_state check (sterilization_state in ('NON_STERILE', 'STERILE_EO', 'STERILE_STEAM', 'STERILE_GAMMA', 'STERILE_E_BEAM')),
  constraint ck_sample_physical_state check (physical_state in ('SOLID', 'LIQUID', 'POWDER', 'OTHER')),
  constraint ck_sample_storage_condition check (storage_condition in ('ROOM_TEMPERATURE', 'REFRIGERATED', 'FROZEN', 'OTHER')),
  constraint ck_sample_status check (status in ('WAITING_DELIVERY', 'ARRIVED_PENDING_CHECK', 'CHECK_EXCEPTION', 'PRE_IN_STOCK', 'IN_STOCK', 'CONDITIONING', 'READY_FOR_TEST', 'IN_TESTING', 'TEST_COMPLETED', 'PENDING_RETURN', 'PENDING_DISPOSAL', 'RETURNED', 'DISPOSED', 'ARCHIVED'))
);

create index ix_sample_submission on sample(submission_id);
create index ix_sample_project on sample(project_id);
create index ix_sample_status on sample(status);
