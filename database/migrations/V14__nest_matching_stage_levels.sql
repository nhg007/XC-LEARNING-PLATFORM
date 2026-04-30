alter table matching_game_sessions
  alter column difficulty type varchar(30);

comment on column matching_game_sessions.difficulty is '关卡编码；可来自连连看难度或消消乐小关配置';

update system_configs
set config_value = (
      select jsonb_build_array(
        jsonb_build_object(
          'code', 'default',
          'labels', jsonb_build_object('zh', '默认关卡', 'en', 'Default', 'ru', 'По умолчанию'),
          'levels', coalesce(
            (
              select jsonb_agg(
                jsonb_build_object(
                  'code', item ->> 'code',
                  'labels', item -> 'labels',
                  'pairCount', item -> 'pairCount',
                  'enabled', coalesce((item ->> 'enabled')::boolean, true),
                  'sortOrder', coalesce((item ->> 'sortOrder')::integer, idx::integer)
                )
                order by idx
              )
              from jsonb_array_elements(config_value::jsonb) with ordinality as legacy(item, idx)
            ),
            '[]'::jsonb
          ),
          'enabled', true,
          'sortOrder', 1
        )
      )::text
    ),
    description = '连连看难度/消消乐关卡组配置。stage 为大关，levels 为小关；小关 code 用于创建游戏，pairCount 为词汇配对组数。',
    updated_at = now()
where config_key = 'matching.stages'
  and jsonb_typeof(config_value::jsonb) = 'array'
  and coalesce((config_value::jsonb -> 0) ? 'pairCount', false);
