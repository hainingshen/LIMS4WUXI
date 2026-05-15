insert into permission_matrix (action_code, role_code, access, scope, note) values
  ('FILE_UPLOAD', 'CUSTOMER_ORG_ADMIN', 'ALLOW', 'ALL', '客户单位管理员上传送检或协议相关附件'),
  ('FILE_UPLOAD', 'CUSTOMER_APPLICANT', 'ALLOW', 'ALL', '客户申请人上传送检或协议相关附件'),
  ('FILE_UPLOAD', 'SAMPLE_MANAGER', 'ALLOW', 'ALL', '样品管理员上传样品照片和核验附件'),
  ('FILE_UPLOAD', 'REPORT_PREPARER', 'ALLOW', 'ALL', '报告编制员上传报告附件'),
  ('FILE_UPLOAD', 'FINANCE_CASHIER', 'ALLOW', 'ALL', '财务上传收款凭证'),
  ('FILE_UPLOAD', 'EQUIPMENT_MANAGER', 'ALLOW', 'ALL', '设备管理员上传设备文件'),
  ('FILE_UPLOAD', 'ARCHIVE_MANAGER', 'ALLOW', 'ALL', '档案管理员上传归档文件'),
  ('FILE_UPLOAD', 'SYSTEM_ADMIN', 'ALLOW', 'ALL', '系统管理员上传系统导入导出文件')
on conflict (action_code, role_code) do update set
  access = excluded.access,
  scope = excluded.scope,
  note = excluded.note;
