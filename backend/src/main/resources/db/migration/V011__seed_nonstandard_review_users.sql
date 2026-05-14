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
    '00000000-0000-0000-0000-000000000103',
    'internal.tech',
    'Internal Technical Leader',
    '{bcrypt}$2a$10$XyKpObntJIQywKuHtbIene/JUxWRBVywMfzkisairCn9BFnteCQ4m',
    'INTERNAL',
    null,
    'ACTIVE'
  ),
  (
    '00000000-0000-0000-0000-000000000104',
    'internal.business',
    'Internal Business Owner',
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
  ('00000000-0000-0000-0000-000000000103', 'TECHNICAL_LEADER'),
  ('00000000-0000-0000-0000-000000000104', 'BUSINESS_OWNER')
on conflict (user_id, role_code) do nothing;
