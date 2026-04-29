alter table media_assets
  add column if not exists status varchar(30) not null default 'active';

alter table media_assets
  drop constraint if exists ck_media_assets_status;

alter table media_assets
  add constraint ck_media_assets_status check (status in ('active', 'inactive'));

comment on column media_assets.status is '媒体状态：active/inactive';

create index if not exists idx_media_assets_status on media_assets(status, media_type, created_at);
