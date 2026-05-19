alter table vocab_items
  add column if not exists stroke_order_asset_id bigint;

alter table vocab_items
  drop constraint if exists fk_vocab_items_stroke_order_asset;

alter table vocab_items
  add constraint fk_vocab_items_stroke_order_asset
    foreign key (stroke_order_asset_id) references media_assets(id);

comment on column vocab_items.stroke_order_asset_id is '汉字笔画演示图片/GIF资源';

create index if not exists idx_vocab_items_stroke_order_asset
  on vocab_items(stroke_order_asset_id);
