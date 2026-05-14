insert into system_config (
  id, config_key, config_value, value_type, category, editable_by_role, description_zh
) values
  ('00000000-0000-0000-0000-000000000c80', 'WORK_CENTER_STALE_DAYS', '2', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '连续无进展预警天数'),
  ('00000000-0000-0000-0000-000000000c81', 'WORK_CENTER_STALE_PENALTY', '20', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '连续无进展扣分'),
  ('00000000-0000-0000-0000-000000000c82', 'WORK_CENTER_NORMAL_SCORE', '85', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '正常健康度最低分'),
  ('00000000-0000-0000-0000-000000000c83', 'WORK_CENTER_RISK_SCORE', '70', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '风险健康度分界'),
  ('00000000-0000-0000-0000-000000000c84', 'WORK_CENTER_OVERDUE_TASK_PENALTY', '8', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '单个超期任务扣分'),
  ('00000000-0000-0000-0000-000000000c85', 'WORK_CENTER_OVERDUE_TASK_MAX_PENALTY', '24', 'INT', 'WORK_CENTER', 'QUALITY_MANAGER', '超期任务最大扣分')
on conflict (config_key) do update set
  config_value = excluded.config_value,
  value_type = excluded.value_type,
  category = excluded.category,
  editable_by_role = excluded.editable_by_role,
  description_zh = excluded.description_zh,
  updated_at = now();
