insert into permission_matrix (action_code, role_code, access, scope, note) values
  ('WORK_CENTER_READ', 'SAMPLE_MANAGER', 'READ', 'TASK', '样品管理员查看收样、入库和留样待办'),
  ('WORK_CENTER_READ', 'INSPECTOR', 'READ', 'TASK', '检测员查看本人工作单和原始记录待办'),
  ('WORK_CENTER_READ', 'REPORT_PREPARER', 'READ', 'TASK', '报告编制员查看报告草稿待办'),
  ('WORK_CENTER_READ', 'AUTHORIZED_SIGNATORY', 'READ', 'TASK', '授权签字人查看待签发报告'),
  ('WORK_CENTER_READ', 'FINANCE_CASHIER', 'READ', 'TASK', '财务收款员查看付款门禁待办'),
  ('WORK_CENTER_READ', 'FINANCE_MANAGER', 'READ', 'TASK', '财务负责人查看付款和绿色通道待办'),
  ('WORK_CENTER_READ', 'EQUIPMENT_MANAGER', 'READ', 'TASK', '设备管理员查看预约和设备文件待办'),
  ('WORK_CENTER_READ', 'INVENTORY_MANAGER', 'READ', 'TASK', '库存管理员查看库存预警待办'),
  ('WORK_CENTER_READ', 'ARCHIVE_MANAGER', 'READ', 'TASK', '档案管理员查看报告归档待办'),
  ('WORK_CENTER_READ', 'SYSTEM_ADMIN', 'READ', 'ALL', '系统管理员查看工作中心用于验收和运维')
on conflict (action_code, role_code) do update set
  access = excluded.access,
  scope = excluded.scope,
  note = excluded.note;
