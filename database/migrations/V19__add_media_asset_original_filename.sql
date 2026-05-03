alter table media_assets
  add column if not exists original_filename varchar(255);

update media_assets
set original_filename = substring(coalesce(object_key, url) from '[^/]+$')
where original_filename is null
  and coalesce(object_key, url) is not null;

comment on column media_assets.original_filename is '上传原始文件名';

create index if not exists idx_media_assets_original_filename
  on media_assets(original_filename);
