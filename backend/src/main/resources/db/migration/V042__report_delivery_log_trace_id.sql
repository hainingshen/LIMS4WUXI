alter table report_delivery_log
  add column if not exists trace_id varchar(64);

create index if not exists ix_report_delivery_log_trace
  on report_delivery_log(trace_id)
  where trace_id is not null;
