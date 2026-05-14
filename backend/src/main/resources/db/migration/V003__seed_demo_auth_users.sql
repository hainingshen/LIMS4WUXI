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
    '00000000-0000-0000-0000-000000000101',
    'internal.admin',
    'Internal Admin',
    '{bcrypt}$2b$10$aSYPVNf7VbR9JqrfKvxja.zIznsStO9y9eSPkB8XvsxSvmJzNEFYq',
    'INTERNAL',
    null,
    'ACTIVE'
  ),
  (
    '00000000-0000-0000-0000-000000000201',
    'portal.admin',
    'Portal Admin',
    '{bcrypt}$2b$10$EdA2KeoOAF9/xEXlEtBfbeeQ15bdPDWxKqaymM6ruD3sA3DOX8bmO',
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
  ('00000000-0000-0000-0000-000000000101', 'SYSTEM_ADMIN'),
  ('00000000-0000-0000-0000-000000000101', 'QUALITY_MANAGER'),
  ('00000000-0000-0000-0000-000000000201', 'CUSTOMER_ORG_ADMIN')
on conflict (user_id, role_code) do nothing;
