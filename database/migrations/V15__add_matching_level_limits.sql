alter table matching_game_sessions
  add column if not exists game_type varchar(30) not null default 'matching';

alter table matching_game_sessions
  add column if not exists time_limit_seconds integer;

alter table matching_game_sessions
  drop constraint if exists ck_matching_game_sessions_game_type;

alter table matching_game_sessions
  add constraint ck_matching_game_sessions_game_type
  check (game_type in ('matching', 'elimination'));

alter table matching_game_sessions
  drop constraint if exists ck_matching_game_sessions_status;

alter table matching_game_sessions
  add constraint ck_matching_game_sessions_status
  check (status in ('playing', 'completed', 'abandoned', 'failed'));

alter table matching_game_sessions
  drop constraint if exists ck_matching_game_sessions_time_limit;

alter table matching_game_sessions
  add constraint ck_matching_game_sessions_time_limit
  check (time_limit_seconds is null or (time_limit_seconds >= 15 and time_limit_seconds <= 3600));

comment on column matching_game_sessions.game_type is '游戏类型：matching 连连看 / elimination 消消乐';
comment on column matching_game_sessions.time_limit_seconds is '关卡限时秒数，可为空表示不限时';
comment on column matching_game_sessions.status is '状态：playing/completed/abandoned/failed';

update system_configs
set config_value = (
      select jsonb_agg(
        jsonb_set(
          stage,
          '{levels}',
          coalesce(
            (
              select jsonb_agg(
                case
                  when level ? 'timeLimitSeconds' then level
                  else jsonb_set(
                    level,
                    '{timeLimitSeconds}',
                    to_jsonb(greatest(45, least(300, coalesce((level ->> 'pairCount')::integer, 4) * 10 + 20)))
                  )
                end
                order by level_index
              )
              from jsonb_array_elements(stage -> 'levels') with ordinality as stage_levels(level, level_index)
            ),
            '[]'::jsonb
          )
        )
        order by stage_index
      )
      from jsonb_array_elements(config_value::jsonb) with ordinality as stages(stage, stage_index)
    ),
    description = '连连看难度/消消乐关卡组配置。stage 为大关，levels 为小关；小关 code 用于创建游戏，pairCount 为词汇配对组数，timeLimitSeconds 为限时秒数。',
    updated_at = now()
where config_key = 'matching.stages'
  and jsonb_typeof(config_value::jsonb) = 'array';
