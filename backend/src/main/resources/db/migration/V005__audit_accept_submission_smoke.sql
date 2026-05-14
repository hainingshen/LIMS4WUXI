insert into app_user (
  id,
  username,
  display_name,
  password_hash,
  client_type,
  customer_org_id,
  status
) values (
  '00000000-0000-0000-0000-000000000102',
  'internal.sales',
  'Internal Sales',
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
  ('00000000-0000-0000-0000-000000000102', 'SALES_REP')
on conflict (user_id, role_code) do nothing;

alter table submission
  add column assigned_sales_user_id uuid references app_user(id);

create index ix_submission_assigned_sales on submission(assigned_sales_user_id);

update submission
set assigned_sales_user_id = '00000000-0000-0000-0000-000000000102'
where assigned_sales_user_id is null;

insert into submission (
  id,
  biz_no,
  customer_org_id,
  applicant_user_id,
  assigned_sales_user_id,
  status,
  created_by,
  updated_by
) values
  (
    '00000000-0000-0000-0000-000000000501',
    'SUB-AUDIT-501',
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000102',
    'SUBMITTED',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000202'
  ),
  (
    '00000000-0000-0000-0000-000000000502',
    'SUB-AUDIT-502',
    '00000000-0000-0000-0000-000000000301',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000102',
    'DRAFT',
    '00000000-0000-0000-0000-000000000202',
    '00000000-0000-0000-0000-000000000202'
  );
