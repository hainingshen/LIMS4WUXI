create table device_booking (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  device_id uuid not null references device(id),
  booker_id uuid not null references app_user(id),
  booker_type varchar(32) not null,
  work_order_id uuid references work_order(id),
  purpose text,
  start_at timestamptz not null,
  end_at timestamptz not null,
  status varchar(32) not null,
  override_reason text,
  overridden_by uuid references app_user(id),
  overridden_at timestamptz,
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
  constraint ck_device_booking_booker_type check (booker_type in ('INSPECTOR', 'EXTERNAL_USER')),
  constraint ck_device_booking_status check (status in (
    'BOOKED', 'OVERRIDDEN', 'NEEDS_RESCHEDULE', 'CANCELLED', 'COMPLETED'
  )),
  constraint ck_device_booking_time_range check (start_at < end_at)
);

create index ix_device_booking_device_time on device_booking(device_id, start_at, end_at);
create index ix_device_booking_booker on device_booking(booker_id, start_at desc);
create index ix_device_booking_status on device_booking(status);
