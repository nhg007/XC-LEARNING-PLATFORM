update exercise_sets existing
set status = 'active',
    updated_at = now()
from (
    values
        ('听音频排序', 'audio_order'),
        ('按拼音排序', 'translation_order'),
        ('听写汉字', 'audio_dictation'),
        ('拼音写句', 'pinyin_dictation')
) as defaults(title, exercise_type)
where existing.parent_id is null
  and existing.title = defaults.title
  and existing.exercise_type = defaults.exercise_type
  and existing.status <> 'active';

insert into exercise_sets (title, exercise_type, level, status, created_at, updated_at)
select defaults.title, defaults.exercise_type, null, 'active', now(), now()
from (
    values
        ('听音频排序', 'audio_order'),
        ('按拼音排序', 'translation_order'),
        ('听写汉字', 'audio_dictation'),
        ('拼音写句', 'pinyin_dictation')
) as defaults(title, exercise_type)
where not exists (
    select 1
    from exercise_sets existing
    where existing.parent_id is null
      and existing.title = defaults.title
      and existing.exercise_type = defaults.exercise_type
);
