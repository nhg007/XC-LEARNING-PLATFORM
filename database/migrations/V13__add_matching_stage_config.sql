alter table system_configs
  drop constraint if exists ck_system_configs_group;

alter table system_configs
  add constraint ck_system_configs_group
  check (config_group in ('payment', 'asr', 'membership', 'upload', 'learning'));

comment on column system_configs.config_group is '配置分组：payment/asr/membership/upload/learning';

alter table matching_game_sessions
  drop constraint if exists ck_matching_game_sessions_difficulty;

alter table matching_game_sessions
  add constraint ck_matching_game_sessions_difficulty
  check (difficulty ~ '^[A-Za-z0-9_-]{1,30}$');

comment on column matching_game_sessions.difficulty is '关卡编码；默认 4x4/7x7/10x10，可通过 system_configs.config_key=matching.stages 配置';

insert into system_configs (
  config_key,
  config_value,
  config_group,
  description,
  created_at,
  updated_at
) values (
  'matching.stages',
  $json$[
    {
      "code": "4x4",
      "labels": {
        "zh": "入门",
        "en": "Easy",
        "ru": "Легко"
      },
      "pairCount": 4,
      "enabled": true,
      "sortOrder": 1
    },
    {
      "code": "7x7",
      "labels": {
        "zh": "进阶",
        "en": "Medium",
        "ru": "Средне"
      },
      "pairCount": 7,
      "enabled": true,
      "sortOrder": 2
    },
    {
      "code": "10x10",
      "labels": {
        "zh": "挑战",
        "en": "Hard",
        "ru": "Сложно"
      },
      "pairCount": 10,
      "enabled": true,
      "sortOrder": 3
    }
  ]$json$,
  'learning',
  '连连看/消消乐关卡配置。code 为关卡编码，labels 为中英俄显示名，pairCount 为词汇配对组数，enabled 控制是否开放。',
  now(),
  now()
) on conflict (config_key) do nothing;
