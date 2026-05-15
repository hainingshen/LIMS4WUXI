update customer_contact cc
set customer_org_id = u.customer_org_id,
    tenant_org_id = u.customer_org_id,
    updated_at = now(),
    updated_by = u.id,
    version = cc.version + 1
from submission s
join app_user u on u.id = s.applicant_user_id
where cc.id = s.customer_contact_id
  and u.client_type = 'PORTAL'
  and u.customer_org_id is not null
  and cc.customer_org_id <> u.customer_org_id
  and cc.deleted_at is null
  and s.deleted_at is null;

update submission s
set customer_org_id = u.customer_org_id,
    tenant_org_id = u.customer_org_id,
    updated_at = now(),
    updated_by = u.id,
    version = s.version + 1
from app_user u
where s.applicant_user_id = u.id
  and u.client_type = 'PORTAL'
  and u.customer_org_id is not null
  and s.customer_org_id <> u.customer_org_id
  and s.deleted_at is null;

update project p
set customer_org_id = u.customer_org_id,
    tenant_org_id = u.customer_org_id,
    updated_at = now(),
    updated_by = u.id,
    version = p.version + 1
from submission s
join app_user u on u.id = s.applicant_user_id
where p.submission_id = s.id
  and u.client_type = 'PORTAL'
  and u.customer_org_id is not null
  and p.customer_org_id <> u.customer_org_id
  and p.deleted_at is null
  and s.deleted_at is null;

update quotation q
set customer_org_id = u.customer_org_id,
    tenant_org_id = u.customer_org_id,
    updated_at = now(),
    updated_by = u.id,
    version = q.version + 1
from project p
join submission s on s.id = p.submission_id
join app_user u on u.id = s.applicant_user_id
where q.project_id = p.id
  and u.client_type = 'PORTAL'
  and u.customer_org_id is not null
  and q.customer_org_id <> u.customer_org_id
  and q.deleted_at is null
  and p.deleted_at is null
  and s.deleted_at is null;

update contract c
set customer_org_id = u.customer_org_id,
    tenant_org_id = u.customer_org_id,
    updated_at = now(),
    updated_by = u.id,
    version = c.version + 1
from project p
join submission s on s.id = p.submission_id
join app_user u on u.id = s.applicant_user_id
where c.project_id = p.id
  and u.client_type = 'PORTAL'
  and u.customer_org_id is not null
  and c.customer_org_id <> u.customer_org_id
  and c.deleted_at is null
  and p.deleted_at is null
  and s.deleted_at is null;
