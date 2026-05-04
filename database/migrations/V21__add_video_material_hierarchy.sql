alter table video_materials
  add column if not exists parent_id bigint;

alter table video_materials
  add constraint fk_video_materials_parent
  foreign key (parent_id) references video_materials(id);

alter table video_materials
  add constraint ck_video_materials_parent_not_self
  check (parent_id is null or parent_id <> id);

comment on column video_materials.parent_id is '上级台词材料 ID；为空表示一级台词材料';

create index if not exists idx_video_materials_parent on video_materials(parent_id, id);
create index if not exists idx_video_materials_status_parent on video_materials(status, parent_id, id);
