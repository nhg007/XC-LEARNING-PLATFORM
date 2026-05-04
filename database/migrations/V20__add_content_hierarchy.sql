alter table vocab_lists
  add column if not exists parent_id bigint;

alter table exercise_sets
  add column if not exists parent_id bigint;

alter table vocab_lists
  add constraint fk_vocab_lists_parent
  foreign key (parent_id) references vocab_lists(id);

alter table exercise_sets
  add constraint fk_exercise_sets_parent
  foreign key (parent_id) references exercise_sets(id);

alter table vocab_lists
  add constraint ck_vocab_lists_parent_not_self
  check (parent_id is null or parent_id <> id);

alter table exercise_sets
  add constraint ck_exercise_sets_parent_not_self
  check (parent_id is null or parent_id <> id);

comment on column vocab_lists.parent_id is '上级词汇表 ID；为空表示一级词汇表';
comment on column exercise_sets.parent_id is '上级题组 ID；为空表示一级题组';

create index if not exists idx_vocab_lists_parent_order on vocab_lists(parent_id, sort_order, id);
create index if not exists idx_vocab_lists_status_parent_order on vocab_lists(status, parent_id, sort_order, id);
create index if not exists idx_exercise_sets_parent on exercise_sets(parent_id, id);
create index if not exists idx_exercise_sets_status_parent on exercise_sets(status, parent_id, id);
