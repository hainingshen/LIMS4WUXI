create table file_metadata (
  id uuid primary key,
  file_name varchar(255) not null,
  object_key varchar(512) not null unique,
  content_type varchar(128),
  size_bytes bigint not null,
  sha256 varchar(64),
  uploaded_by uuid references app_user(id),
  uploaded_at timestamptz not null default now(),
  retention_until date,
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
  data_quality_score int
);

create index ix_file_metadata_uploaded_by on file_metadata(uploaded_by);
create index ix_file_metadata_uploaded_at on file_metadata(uploaded_at);

create table sample_photo (
  id uuid primary key,
  sample_id uuid not null references sample(id) on delete cascade,
  file_id uuid not null references file_metadata(id),
  photo_type varchar(32) not null,
  note text,
  uploaded_at timestamptz not null default now(),
  uploaded_by uuid references app_user(id),
  sort_order int not null default 0
);

create index ix_sample_photo_sample on sample_photo(sample_id, sort_order);
create index ix_sample_photo_file on sample_photo(file_id);
