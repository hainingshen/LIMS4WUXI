insert into business_rule (
  id,
  rule_code,
  name_zh,
  trigger_type,
  condition_expr,
  action_code,
  action_payload,
  priority,
  enabled,
  note
) values
  (
    '00000000-0000-0000-0000-000000001001',
    'PLAN_REQUIRES_QA',
    'GLP方案要求QA审批',
    'STUDY_PLAN_REVIEW',
    'requiresGlp == true',
    'REQUIRE_QA_REVIEW',
    '{"stage":"QA"}'::jsonb,
    100,
    true,
    'GLP 项目方案审批必须经过 QA'
  ),
  (
    '00000000-0000-0000-0000-000000001002',
    'PLAN_REQUIRES_ETHICS',
    '动物试验方案要求伦理审批',
    'STUDY_PLAN_REVIEW',
    'requiresAnimalTest == true',
    'REQUIRE_ETHICS_REVIEW',
    '{"stage":"ETHICS"}'::jsonb,
    110,
    true,
    '动物试验方案审批必须经过伦理委员会'
  )
on conflict (rule_code) do update set
  name_zh = excluded.name_zh,
  trigger_type = excluded.trigger_type,
  condition_expr = excluded.condition_expr,
  action_code = excluded.action_code,
  action_payload = excluded.action_payload,
  priority = excluded.priority,
  enabled = excluded.enabled,
  note = excluded.note,
  updated_at = now();
