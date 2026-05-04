create table if not exists user_sentence_favorites (
  id bigserial,
  user_id bigint not null,
  sentence_exercise_id bigint not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint pk_user_sentence_favorites primary key (id),
  constraint fk_user_sentence_favorites_user foreign key (user_id) references users(id),
  constraint fk_user_sentence_favorites_exercise foreign key (sentence_exercise_id) references sentence_exercises(id),
  constraint uq_user_sentence_favorites_user_exercise unique (user_id, sentence_exercise_id)
);

comment on table user_sentence_favorites is '句子收藏表';
comment on column user_sentence_favorites.id is '收藏 ID';
comment on column user_sentence_favorites.user_id is '用户 ID';
comment on column user_sentence_favorites.sentence_exercise_id is '句子题 ID';
comment on column user_sentence_favorites.created_at is '收藏时间';
comment on column user_sentence_favorites.updated_at is '更新时间';

create index if not exists idx_user_sentence_favorites_user on user_sentence_favorites(user_id);
create index if not exists idx_user_sentence_favorites_exercise on user_sentence_favorites(sentence_exercise_id);
