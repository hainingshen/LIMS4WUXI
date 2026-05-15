alter table report_delivery_log
  drop constraint if exists ck_report_delivery_type;

update report_delivery_log
set delivery_type = 'GREEN_CHANNEL_RELEASE'
where delivery_type = 'GREEN_CHANNEL';

update report_delivery_log
set delivery_type = case
  when client_type = 'INTERNAL' then 'INTERNAL_DOWNLOAD'
  else 'OFFICIAL_DOWNLOAD'
end
where delivery_type = 'DOWNLOAD';

alter table report_delivery_log
  add constraint ck_report_delivery_type check (delivery_type in (
    'PREVIEW',
    'OFFICIAL_DOWNLOAD',
    'INTERNAL_DOWNLOAD',
    'EMAIL',
    'BATCH_DOWNLOAD',
    'GREEN_CHANNEL_RELEASE',
    'PAYMENT_UNLOCK_RELEASE'
  ));

delete from dictionary_item
where dict_code = 'REPORT_DELIVERY_TYPE'
  and item_code in ('DOWNLOAD', 'GREEN_CHANNEL');

insert into dictionary_item (dict_code, item_code, label_zh, label_en, sort_order, enabled)
values
  ('REPORT_DELIVERY_TYPE', 'PREVIEW', '预览', 'Preview', 1, true),
  ('REPORT_DELIVERY_TYPE', 'OFFICIAL_DOWNLOAD', '正式报告下载', 'Official download', 2, true),
  ('REPORT_DELIVERY_TYPE', 'INTERNAL_DOWNLOAD', '内部下载', 'Internal download', 3, true),
  ('REPORT_DELIVERY_TYPE', 'EMAIL', '邮件发送', 'Email', 4, true),
  ('REPORT_DELIVERY_TYPE', 'BATCH_DOWNLOAD', '批量下载', 'Batch download', 5, true),
  ('REPORT_DELIVERY_TYPE', 'GREEN_CHANNEL_RELEASE', '绿色通道放行', 'Green channel release', 6, true),
  ('REPORT_DELIVERY_TYPE', 'PAYMENT_UNLOCK_RELEASE', '付款解锁放行', 'Payment unlock release', 7, true)
on conflict (dict_code, item_code) do update set
  label_zh = excluded.label_zh,
  label_en = excluded.label_en,
  sort_order = excluded.sort_order,
  enabled = excluded.enabled;
