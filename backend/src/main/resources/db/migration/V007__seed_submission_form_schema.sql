with field_specs (
  sort_order,
  part,
  field_key,
  label_zh,
  label_en,
  field_type,
  required,
  dictionary,
  entity,
  notes,
  lock_policy
) as (
values
  (1, 'PART1', 'sponsorNameZh', '委托单位中文', 'Sponsor Chinese', 'string', true, '', 'customer_org', '中英文双字段', 'CHANGE_REQUEST'),
  (2, 'PART1', 'sponsorNameEn', '委托单位英文', 'Sponsor English', 'string', true, '', 'customer_org', '中英文双字段', 'CHANGE_REQUEST'),
  (3, 'PART1', 'sponsorAddressZh', '委托单位地址中文', 'Sponsor address Chinese', 'text', true, '', 'customer_org', '中英文双字段', 'CHANGE_REQUEST'),
  (4, 'PART1', 'sponsorAddressEn', '委托单位地址英文', 'Sponsor address English', 'text', true, '', 'customer_org', '中英文双字段', 'CHANGE_REQUEST'),
  (5, 'PART1', 'manufacturerNameZh', '制造商中文', 'Manufacturer Chinese', 'string', true, '', 'manufacturer', '中英文双字段', 'CHANGE_REQUEST'),
  (6, 'PART1', 'manufacturerNameEn', '制造商英文', 'Manufacturer English', 'string', true, '', 'manufacturer', '中英文双字段', 'CHANGE_REQUEST'),
  (7, 'PART1', 'manufacturerAddressZh', '制造商地址中文', 'Manufacturer address Chinese', 'text', true, '', 'manufacturer', '中英文双字段', 'CHANGE_REQUEST'),
  (8, 'PART1', 'manufacturerAddressEn', '制造商地址英文', 'Manufacturer address English', 'text', true, '', 'manufacturer', '中英文双字段', 'CHANGE_REQUEST'),
  (9, 'PART1', 'contactName', '联系人', 'ATTN', 'string', true, '', 'customer_contact', '', 'CHANGE_REQUEST'),
  (10, 'PART1', 'contactPhone', '电话', 'Tel', 'string', true, '', 'customer_contact', '', 'CHANGE_REQUEST'),
  (11, 'PART1', 'contactEmail', '联系人邮箱', 'E-mail', 'string', true, '', 'customer_contact', '', 'CHANGE_REQUEST'),
  (12, 'PART1', 'officialReportEmail', '电子报告官方接收邮箱', 'Official email', 'string', true, '', 'submission', '可多邮箱', 'CHANGE_REQUEST'),
  (13, 'PART1', 'shippingInfo', '报告/发票寄送信息', 'Shipping info', 'text', true, '', 'submission', '', 'CHANGE_REQUEST'),
  (14, 'PART2', 'requiresGlp', '是否需要GLP报告', 'GLP Reports or not', 'boolean', true, 'YES_NO', 'submission', '', 'CHANGE_REQUEST'),
  (15, 'PART2', 'requiresHardCopyOfElectronicReport', '是否需要电子报告纸质盖章复印件', 'Need hard copies', 'boolean', true, 'YES_NO', 'submission', '', 'CHANGE_REQUEST'),
  (16, 'PART2', 'protocolLanguage', '计划书语言', 'Language of Protocols', 'enum', true, 'LANGUAGE_CHS_ENG_BOTH', 'submission', '', 'CHANGE_REQUEST'),
  (17, 'PART2', 'reportLanguage', '报告语言', 'Language of Reports', 'enum', true, 'LANGUAGE_CHS_ENG_BOTH', 'submission', '', 'CHANGE_REQUEST'),
  (18, 'PART3', 'submittedQuantityText', '送检样品总数', 'Quantity submitted', 'string', true, '', 'submission', '保留客户原始描述', 'CHANGE_REQUEST'),
  (19, 'PART3', 'sampleDisposition', '样品处置', 'Test Article disposition', 'enum', true, 'SAMPLE_DISPOSITION', 'submission', '', 'CHANGE_REQUEST'),
  (20, 'PART3', 'sampleDispositionRemark', '其他处置说明', 'Disposition remark', 'text', false, '', 'submission', '处置为OTHER时必填', 'FREELY_EDITABLE'),
  (21, 'PART3', 'recipientName', '样品接收人', 'Recipient', 'string', false, '', 'sample', '内部填写', 'FREELY_EDITABLE'),
  (22, 'PART3', 'receivedDate', '样品接收日期', 'Received date', 'date', false, '', 'sample', '内部填写', 'FREELY_EDITABLE'),
  (23, 'PART4', 'sampleNameZh', '样品名称中文', 'Sample name Chinese', 'string', true, '', 'sample', '中英文双字段', 'CHANGE_REQUEST'),
  (24, 'PART4', 'sampleNameEn', '样品名称英文', 'Sample name English', 'string', true, '', 'sample', '中英文双字段', 'CHANGE_REQUEST'),
  (25, 'PART4', 'sterilizationState', '灭菌状态', 'Sterilization state', 'enum', true, 'STERILIZATION_STATE', 'sample', '', 'CHANGE_REQUEST'),
  (26, 'PART4', 'casCode', 'CAS号', 'CAS code', 'string', true, '', 'sample', '不适用填N/A', 'CHANGE_REQUEST'),
  (27, 'PART4', 'modelRef', '型号', 'Model/REF', 'string', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (28, 'PART4', 'sizeSpec', '规格', 'Size', 'string', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (29, 'PART4', 'lotBatch', '批号', 'LOT/Batch', 'string', true, '', 'sample', '报告后不可更改', 'HARD_LOCK'),
  (30, 'PART4', 'rawMaterial', '样品材料', 'Raw material', 'text', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (31, 'PART4', 'packagingMaterial', '包装材料', 'Packaging material', 'text', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (32, 'PART4', 'physicalState', '物理状态', 'Physical state', 'enum', true, 'PHYSICAL_STATE', 'sample', '', 'CHANGE_REQUEST'),
  (33, 'PART4', 'color', '颜色', 'Color', 'string', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (34, 'PART4', 'density', '密度', 'Density', 'string', true, '', 'sample', '保留单位', 'CHANGE_REQUEST'),
  (35, 'PART4', 'stability', '稳定性', 'Stability', 'text', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (36, 'PART4', 'solubility', '溶解度', 'Solubility', 'text', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (37, 'PART4', 'storageCondition', '保存条件', 'Storage condition', 'enum', true, 'STORAGE_CONDITION', 'sample', '', 'CHANGE_REQUEST'),
  (38, 'PART4', 'intendedUse', '预期用途', 'Intended use', 'text', true, '', 'sample', '', 'CHANGE_REQUEST'),
  (39, 'PART4', 'additionalInfo', '附加信息', 'Additional information', 'text', true, '', 'sample', '进入报告', 'CHANGE_REQUEST'),
  (40, 'PART5', 'pretreatmentRequired', '样品前处理要求', 'Pretreatments required', 'text', false, '', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (41, 'PART5', 'nonSterileExtraProcedure', '非灭菌样品额外消毒或灭菌程序', 'Additional procedures', 'text', false, '', 'submission_biocompatibility', '非灭菌样品条件必填', 'CHANGE_REQUEST'),
  (42, 'PART5', 'canBeCut', '样品是否可被切割', 'Can be cut or not', 'boolean', true, 'YES_NO', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (43, 'PART5', 'componentsToTestZh', '指定检测部位或材料中文', 'Components/materials Chinese', 'text', true, '', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (44, 'PART5', 'componentsToTestEn', '指定检测部位或材料英文', 'Components/materials English', 'text', true, '', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (45, 'PART5', 'workingTemperatureCelsius', '工作温度', 'Working temperature', 'decimal_or_text', true, '', 'submission_biocompatibility', '摄氏度', 'CHANGE_REQUEST'),
  (46, 'PART5', 'withstand50C', '是否耐受50°C', 'Can withstand 50°C', 'boolean', true, 'YES_NO', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (47, 'PART5', 'clinicalContactDuration', '累积接触时长', 'Clinical accumulative contact duration', 'enum', true, 'CONTACT_DURATION', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (48, 'PART5', 'extractionBasis', '浸提液制备依据', 'To be extracted basing on', 'enum', true, 'EXTRACTION_BASIS', 'submission_biocompatibility', '', 'CHANGE_REQUEST'),
  (49, 'PART5', 'standardSurfaceAreaCm2', '标准表面积', 'Standard surface area', 'decimal_or_text', false, '', 'submission_biocompatibility', '表面积浸提时必填', 'CHANGE_REQUEST'),
  (50, 'PART5', 'thinnestThicknessMm', '最薄处厚度', 'Thickness at thinnest part', 'decimal_or_text', true, '', 'submission_biocompatibility', '毫米', 'CHANGE_REQUEST'),
  (51, 'PART5', 'specialInstructions', '其他特别说明', 'Special instructions', 'text', true, '', 'submission_biocompatibility', '无则N/A', 'CHANGE_REQUEST'),
  (52, 'PART6', 'glpCharacterizationAffirmed', 'GLP样品表征确认', 'Test article characterization affirmed', 'boolean', false, 'YES_NO', 'submission', 'requiresGlp=true时必填', 'CHANGE_REQUEST'),
  (53, 'PART6', 'glpCharacterizationStatementVersion', 'GLP声明版本', 'Statement version', 'string', false, '', 'submission', '', 'CHANGE_REQUEST'),
  (54, 'PART6', 'glpCharacterizationConfirmedBy', 'GLP声明确认人', 'Confirmed by', 'string', false, '', 'submission', '', 'CHANGE_REQUEST'),
  (55, 'PART6', 'glpCharacterizationConfirmedAt', 'GLP声明确认时间', 'Confirmed at', 'datetime', false, '', 'submission', '', 'CHANGE_REQUEST'),
  (56, 'PART7', 'biocompatibilitySelections', '生物相容性试验选择', 'Biocompatibility selections', 'array', false, '', 'submission_test_selection', '明细表保存', 'CHANGE_REQUEST'),
  (57, 'PART8', 'microPackagingPhysicalSelections', '微生物/包装/物理试验选择', 'Micro packaging physical selections', 'array', false, '', 'submission_test_selection', '明细表保存', 'CHANGE_REQUEST'),
  (58, 'PART8', 'otherTestSelections', '其他试验', 'Other tests', 'array', false, '', 'submission_test_selection', '客户自填项目', 'CHANGE_REQUEST')
),
field_json as (
  select
    sort_order,
    part,
    field_key,
    required,
    jsonb_build_object(
      'part', part,
      'fieldKey', field_key,
      'labelZh', label_zh,
      'labelEn', label_en,
      'type', field_type,
      'required', required,
      'dictionary', nullif(dictionary, ''),
      'entity', entity,
      'notes', nullif(notes, ''),
      'lockPolicy', lock_policy
    ) as field_spec,
    jsonb_strip_nulls(jsonb_build_object(
      'type',
        case
          when field_type = 'boolean' then 'boolean'
          when field_type = 'array' then 'array'
          else 'string'
        end,
      'title', label_zh,
      'x-label-en', label_en,
      'x-part', part,
      'x-field-type', field_type,
      'x-entity', entity,
      'x-lock-policy', lock_policy,
      'x-dictionary', nullif(dictionary, ''),
      'description', nullif(notes, ''),
      'format',
        case
          when field_type = 'date' then 'date'
          when field_type = 'datetime' then 'date-time'
          else null
        end,
      'items',
        case
          when field_type = 'array' then jsonb_build_object('type', 'object', 'additionalProperties', true)
          else null
        end
    )) as property_schema
  from field_specs
),
part_json as (
  select
    part,
    min(sort_order) as sort_order,
    jsonb_agg(field_key order by sort_order) as fields
  from field_json
  group by part
)
insert into form_schema (
  id,
  form_code,
  version_no,
  schema_json,
  ui_schema_json,
  status,
  published_at,
  note
)
select
  '00000000-0000-0000-0000-000000000701',
  'SUBMISSION_VER15',
  1,
  jsonb_build_object(
    'type', 'object',
    'title', 'Submission Ver.15',
    'x-form-code', 'SUBMISSION_VER15',
    'x-schema-version', 1,
    'fields', (select jsonb_agg(field_spec order by sort_order) from field_json),
    'properties', (select jsonb_object_agg(field_key, property_schema order by sort_order) from field_json),
    'required', (select jsonb_agg(field_key order by sort_order) from field_json where required)
  ),
  jsonb_build_object(
    'layout', 'PART_GROUPED',
    'parts', (
      select jsonb_agg(jsonb_build_object('part', part, 'fields', fields) order by sort_order)
      from part_json
    )
  ),
  'PUBLISHED',
  now(),
  'Seeded from docs/development/specs/submission_ver15_fields.csv';
