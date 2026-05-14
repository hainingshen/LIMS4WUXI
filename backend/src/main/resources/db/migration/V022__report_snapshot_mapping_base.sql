create table report_field_mapping (
  id uuid primary key,
  template_code varchar(64) not null,
  target_field varchar(128) not null,
  report_label varchar(255) not null,
  source_entity varchar(64) not null,
  source_field_or_rule text not null,
  required boolean not null,
  auto_fill boolean not null,
  notes text,
  sort_order int not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ux_report_field_mapping unique (template_code, target_field)
);

create index ix_report_field_mapping_template on report_field_mapping(template_code, sort_order);

create table report_work_order (
  report_id uuid not null references report(id) on delete cascade,
  work_order_id uuid not null references work_order(id),
  raw_record_id uuid references raw_record(id),
  created_at timestamptz not null default now(),
  primary key (report_id, work_order_id)
);

create index ix_report_work_order_work_order on report_work_order(work_order_id);
create index ix_report_work_order_raw_record on report_work_order(raw_record_id);

insert into report_field_mapping (
  id, template_code, target_field, report_label, source_entity,
  source_field_or_rule, required, auto_fill, notes, sort_order
) values
  ('00000000-0000-0000-0000-000000000e01', 'STERILITY_REPORT', 'reportNo', '报告编号', 'report', 'bizNo', true, true, 'TR编号', 1),
  ('00000000-0000-0000-0000-000000000e02', 'STERILITY_REPORT', 'entrustingUnit', '委托单位', 'customer_org', 'nameZh', true, true, null, 2),
  ('00000000-0000-0000-0000-000000000e03', 'STERILITY_REPORT', 'entrustingAddress', '单位地址', 'customer_org', 'addressZh', true, true, null, 3),
  ('00000000-0000-0000-0000-000000000e04', 'STERILITY_REPORT', 'productionUnit', '生产单位', 'submission', 'manufacturerNameZh', false, true, '无则填/', 4),
  ('00000000-0000-0000-0000-000000000e05', 'STERILITY_REPORT', 'productionUnitAddress', '生产单位地址', 'submission', 'manufacturerAddressZh', false, true, '无则填/', 5),
  ('00000000-0000-0000-0000-000000000e06', 'STERILITY_REPORT', 'sampleName', '样品名称', 'sample', 'sampleNameZh', true, true, null, 6),
  ('00000000-0000-0000-0000-000000000e07', 'STERILITY_REPORT', 'sampleNo', '样品编号', 'sample', 'bizNo', true, true, '迁移项目可用legacy sample no', 7),
  ('00000000-0000-0000-0000-000000000e08', 'STERILITY_REPORT', 'sampleLotProductionDate', '样品批号/生产日期', 'sample', 'lotBatch + ''/'' + productionDate', true, true, null, 8),
  ('00000000-0000-0000-0000-000000000e09', 'STERILITY_REPORT', 'samplingDate', '送样日期', 'sample', 'receivedAt or submittedSamplingDate', true, true, null, 9),
  ('00000000-0000-0000-0000-000000000e0a', 'STERILITY_REPORT', 'type', '规格型号', 'sample', 'modelRef or sizeSpec', true, true, null, 10),
  ('00000000-0000-0000-0000-000000000e0b', 'STERILITY_REPORT', 'sumOfSample', '样品数量', 'sample', 'quantity + quantityUnit', true, true, null, 11),
  ('00000000-0000-0000-0000-000000000e0c', 'STERILITY_REPORT', 'sampleState', '样品状态', 'sample', 'physicalState', true, true, null, 12),
  ('00000000-0000-0000-0000-000000000e0d', 'STERILITY_REPORT', 'testDate', '检测日期', 'work_order', 'min(startedAt) + ''-'' + max(completedAt)', true, true, null, 13),
  ('00000000-0000-0000-0000-000000000e0e', 'STERILITY_REPORT', 'testItems', '检测项目', 'raw_record', 'testItem', true, true, '无菌', 14),
  ('00000000-0000-0000-0000-000000000e0f', 'STERILITY_REPORT', 'testBasis', '检测依据', 'test_standard', 'selected test_standard.name_zh + standard_no from test_item_standard where usage_type=TEST_BASIS', true, true, '例 YY/T 1293.4-2016', 15),
  ('00000000-0000-0000-0000-000000000e10', 'STERILITY_REPORT', 'testEquipment', '检测设备', 'raw_record', 'equipmentUsages[].name + ''('' + no + '')''', true, true, null, 16),
  ('00000000-0000-0000-0000-000000000e11', 'STERILITY_REPORT', 'conclusion', '检测结论', 'raw_record', 'sterilityResults[].conclusion', true, true, '人工可改', 17),
  ('00000000-0000-0000-0000-000000000e12', 'STERILITY_REPORT', 'remark', '备注', 'report', 'remark', false, false, '默认空', 18),
  ('00000000-0000-0000-0000-000000000e13', 'STERILITY_REPORT', 'preparedBy', '编制', 'workflow', 'report_preparer signature', true, true, null, 19),
  ('00000000-0000-0000-0000-000000000e14', 'STERILITY_REPORT', 'checkedBy', '审核', 'workflow', 'QC reviewer signature', true, true, null, 20),
  ('00000000-0000-0000-0000-000000000e15', 'STERILITY_REPORT', 'approvedBy', '批准', 'workflow', 'SD or authorized signer signature', true, true, null, 21),
  ('00000000-0000-0000-0000-000000000e16', 'STERILITY_REPORT', 'watermark', '水印', 'report', 'watermarkType', true, true, 'DRAFT/TEST/NONE', 22),
  ('00000000-0000-0000-0000-000000000e17', 'STERILITY_REPORT', 'seal', '电子签章', 'report', 'sealStatus', true, true, '正式版且付款门禁通过后加盖', 23);
