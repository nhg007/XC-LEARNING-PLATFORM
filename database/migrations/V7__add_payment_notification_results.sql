alter table if exists payment_notifications
  add column if not exists process_status varchar(30),
  add column if not exists result_code varchar(80),
  add column if not exists result_message varchar(500);

update payment_notifications
set process_status = case when handled then 'handled' else 'failed' end
where process_status is null;

alter table if exists payment_notifications
  alter column process_status set default 'handled',
  alter column process_status set not null;

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'ck_payment_notifications_process_status'
  ) then
    alter table payment_notifications
      add constraint ck_payment_notifications_process_status
      check (process_status in ('handled', 'ignored', 'failed'));
  end if;
end $$;

comment on column payment_notifications.process_status is '回调处理状态：handled/ignored/failed';
comment on column payment_notifications.result_code is '回调处理结果码';
comment on column payment_notifications.result_message is '回调处理结果说明';

create index if not exists idx_payment_notifications_process_status
  on payment_notifications(process_status, received_at);
