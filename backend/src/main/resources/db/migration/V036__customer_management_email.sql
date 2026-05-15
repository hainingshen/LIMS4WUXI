alter table app_user
  add column if not exists email varchar(255);

create index if not exists ix_app_user_customer_org on app_user(customer_org_id);
create index if not exists ix_app_user_email on app_user(email);
