alter table if exists user_memberships
  add column if not exists payment_order_id bigint;

comment on column user_memberships.payment_order_id is '来源为支付时对应的支付订单 ID';

do $$
begin
  if not exists (
    select 1
    from pg_constraint
    where conname = 'fk_user_memberships_payment_order'
  ) then
    alter table user_memberships
      add constraint fk_user_memberships_payment_order
      foreign key (payment_order_id) references payment_orders(id);
  end if;
end $$;

create unique index if not exists uq_user_memberships_payment_order
  on user_memberships(payment_order_id)
  where payment_order_id is not null;
