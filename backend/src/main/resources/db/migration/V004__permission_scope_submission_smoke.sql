insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values
  (
    '00000000-0000-0000-0000-000000000202',
    'portal.applicant',
    'Portal Applicant',
    '{bcrypt}$2a$10$bUI33Tb5dPXZgrB6o8/2J.To.pygo8WUIwpQ0gSTGqA43xe5yXMmS',
    'PORTAL',
    '00000000-0000-0000-0000-000000000301',
    'ACTIVE'
  ),
  (
    '00000000-0000-0000-0000-000000000203',
    'portal.viewer',
    'Portal Viewer',
    '{bcrypt}$2a$10$SdbzYzCpHsyx0bgU7umsueWzJbXgwRmiC5neJdkNZtWq5BPTvtrma',
    'PORTAL',
    '00000000-0000-0000-0000-000000000301',
    'ACTIVE'
  )
on conflict (username) do update set
  display_name = excluded.display_name,
  password_hash = excluded.password_hash,
  client_type = excluded.client_type,
  customer_org_id = excluded.customer_org_id,
  status = excluded.status;

insert into user_role (user_id, role_code) values
  ('00000000-0000-0000-0000-000000000202', 'CUSTOMER_APPLICANT'),
  ('00000000-0000-0000-0000-000000000203', 'CUSTOMER_VIEWER')
on conflict (user_id, role_code) do nothing;

create table submission (
  id uuid primary key,
  biz_no varchar(64) not null unique,
  customer_org_id uuid not null,
  applicant_user_id uuid not null references app_user(id),
  status varchar(32) not null,
  created_at timestamptz not null default now(),
  created_by uuid,
  updated_at timestamptz not null default now(),
  updated_by uuid,
  deleted_at timestamptz,
  version int not null default 0,
  constraint ck_submission_status check (status in ('DRAFT', 'SUBMITTED', 'ACCEPTED', 'PROJECT_SPLIT', 'CANCELLED'))
);

create index ix_submission_customer_org on submission(customer_org_id);
create index ix_submission_applicant on submission(applicant_user_id);
create index ix_submission_status on submission(status);

insert into submission (
  id,
  biz_no,
  customer_org_id,
  applicant_user_id,
  status,
  created_by,
  updated_by
) values
  (
    '00000000-0000-0000-0000-000000000401',
    'SUB-PERM-401',
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000202',
    'DRAFT',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000202'
  ),
  (
    '00000000-0000-0000-0000-000000000402',
    'SUB-PERM-402',
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000202',
    'DRAFT',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000202'
  );
