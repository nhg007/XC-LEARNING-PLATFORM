alter table media_assets
  add column if not exists object_key varchar(500);

update media_assets
set object_key = substring(url from length('/api/media/') + 1)
where object_key is null
  and url like '/api/media/%';

comment on column media_assets.object_key is '对象存储 key';

create unique index if not exists uq_media_assets_object_key
  on media_assets(object_key)
  where object_key is not null;
