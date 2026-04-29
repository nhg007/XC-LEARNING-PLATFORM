alter table if exists payment_orders
  drop constraint if exists ck_payment_orders_provider;

alter table if exists payment_orders
  add constraint ck_payment_orders_provider
  check (provider in ('wechat_pay', 'alipay', 'offline'));

comment on column payment_orders.provider is '支付渠道：wechat_pay/alipay/offline';
