create table report_delivery_log (
  id uuid primary key,
  report_id uuid not null references report(id),
  report_version_id uuid not null references report_version(id),
  delivery_type varchar(32) not null,
  delivered_to varchar(512),
  delivered_by uuid references app_user(id),
  delivered_at timestamptz not null,
  client_type varchar(32) not null,
  payment_gate_status varchar(32) not null,
  file_id uuid references file_metadata(id),
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
  constraint ck_report_delivery_type check (delivery_type in (
    'PREVIEW', 'DOWNLOAD', 'EMAIL', 'BATCH_DOWNLOAD', 'GREEN_CHANNEL'
  )),
  constraint ck_report_delivery_client_type check (client_type in ('INTERNAL', 'PORTAL', 'MOBILE', 'SYSTEM')),
  constraint ck_report_delivery_gate_status check (payment_gate_status in ('LOCKED', 'UNLOCKED', 'GREEN_UNLOCKED'))
);

create index ix_report_delivery_log_report on report_delivery_log(report_id, delivered_at desc);
create index ix_report_delivery_log_version on report_delivery_log(report_version_id);
create index ix_report_delivery_log_type on report_delivery_log(delivery_type, delivered_at desc);
