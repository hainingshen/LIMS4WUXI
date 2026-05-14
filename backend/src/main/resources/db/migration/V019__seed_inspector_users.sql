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
    '00000000-0000-0000-0000-000000000107',
    'internal.inspector',
    'Internal Inspector',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    null,
    'ACTIVE'
  ),
  (
    '00000000-0000-0000-0000-000000000108',
    'internal.inspector2',
    'Internal Inspector 2',
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
  ('00000000-0000-0000-0000-000000000107', 'INSPECTOR'),
  ('00000000-0000-0000-0000-000000000108', 'INSPECTOR')
on conflict (user_id, role_code) do nothing;
