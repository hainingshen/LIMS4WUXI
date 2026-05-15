insert into organization_unit (id, parent_id, name, status)
values ('00000000-0000-0000-0000-000000000a01', null, '无锡锡山实验室', 'ACTIVE')
on conflict (id) do update set
  name = excluded.name,
  status = excluded.status,
  updated_at = now();

create table holiday (
  id uuid primary key,
  holiday_date date not null unique,
  type varchar(32) not null,
  name varchar(128) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ck_holiday_type check (type in ('HOLIDAY', 'WORKDAY'))
);

create index ix_holiday_date on holiday(holiday_date);

insert into holiday (id, holiday_date, type, name)
values
  ('00000000-0000-0000-0000-000000000a11', '2026-01-01', 'HOLIDAY', '元旦'),
  ('00000000-0000-0000-0000-000000000a12', '2026-05-01', 'HOLIDAY', '劳动节')
on conflict (holiday_date) do update set
  type = excluded.type,
  name = excluded.name,
  updated_at = now();

create table legacy_status_mapping (
  legacy_entity_type varchar(64) not null,
  legacy_status_text varchar(128) not null,
  target_entity_type varchar(64) not null,
  target_status varchar(64) not null,
  migration_mode varchar(32) not null,
  quality_score_delta int,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (legacy_entity_type, legacy_status_text),
  constraint ck_legacy_status_mapping_mode check (
    migration_mode in ('ACTIVE_FLOW', 'READONLY_ARCHIVE', 'MANUAL_REVIEW')
  )
);

insert into legacy_status_mapping (
  legacy_entity_type, legacy_status_text, target_entity_type, target_status,
  migration_mode, quality_score_delta, note
) values
  ('SUBMISSION', '待提交', 'SUBMISSION', 'DRAFT', 'ACTIVE_FLOW', 0, '旧送检单待提交映射为草稿'),
  ('REPORT', '已签发未付款', 'REPORT', 'ISSUED_PAYMENT_LOCKED', 'ACTIVE_FLOW', 0, '旧报告付款锁定状态映射')
on conflict (legacy_entity_type, legacy_status_text) do update set
  target_entity_type = excluded.target_entity_type,
  target_status = excluded.target_status,
  migration_mode = excluded.migration_mode,
  quality_score_delta = excluded.quality_score_delta,
  note = excluded.note,
  updated_at = now();
