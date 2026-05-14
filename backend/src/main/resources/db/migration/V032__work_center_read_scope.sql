insert into permission_matrix (action_code, role_code, access, scope, note) values
  ('WORK_CENTER_READ', 'SALES_REP', 'READ', 'PROJECT_PARTICIPANT', '业务员查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'BUSINESS_OWNER', 'READ', 'PROJECT_PARTICIPANT', '业务负责人查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'TECHNICAL_LEADER', 'READ', 'PROJECT_PARTICIPANT', '技术负责人查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'SD', 'READ', 'PROJECT_PARTICIPANT', 'SD查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'QA', 'READ', 'PROJECT_PARTICIPANT', 'QA查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'QC', 'READ', 'PROJECT_PARTICIPANT', 'QC查看本人相关项目工作中心'),
  ('WORK_CENTER_READ', 'DEPT_MANAGER', 'READ', 'ALL', '部门负责人查看工作中心'),
  ('WORK_CENTER_READ', 'GENERAL_MANAGER', 'READ', 'ALL', '管理层查看工作中心')
on conflict (action_code, role_code) do update set
  access = excluded.access,
  scope = excluded.scope,
  note = excluded.note;
