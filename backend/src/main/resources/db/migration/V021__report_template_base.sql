create table report_template (
  id uuid primary key,
  template_code varchar(64) not null,
  template_version varchar(32) not null,
  template_name varchar(255) not null,
  template_type varchar(64) not null,
  automation_level varchar(16) not null,
  file_id uuid not null references file_metadata(id),
  status varchar(32) not null,
  published_at timestamptz,
  published_by uuid references app_user(id),
  legacy_template_name varchar(255),
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
  constraint ux_report_template_code_version unique (template_code, template_version),
  constraint ck_report_template_type check (template_type in (
    'CONTRACT_EXPORT', 'SAMPLE_FLOW_EXPORT', 'REPORT', 'EN_REPORT', 'SPECIAL_REPORT'
  )),
  constraint ck_report_template_automation_level check (automation_level in ('A', 'B', 'C')),
  constraint ck_report_template_status check (status in ('DRAFT', 'PUBLISHED', 'DEPRECATED', 'DISABLED'))
);

create unique index ux_report_template_published
  on report_template(template_code)
  where status = 'PUBLISHED' and deleted_at is null;

create index ix_report_template_code on report_template(template_code);
create index ix_report_template_status on report_template(status);
create index ix_report_template_file on report_template(file_id);

alter table report
  add constraint fk_report_template
  foreign key (report_template_id) references report_template(id);
