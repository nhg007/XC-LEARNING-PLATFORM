# 数据库状态值与枚举值

本文件集中整理 PostgreSQL DDL 中出现的状态值和枚举值。后端、前端和测试用例必须以这里为准。

## 通用状态

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `status` | `active` | 启用、有效、正常。 |
| `status` | `inactive` | 停用、下架。 |
| `status` | `disabled` | 账号禁用。 |
| `status` | `deleted` | 逻辑删除。 |
| `status` | `archived` | 已归档。 |

## 用户与会员

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `users.status` | `active` | 正常用户。 |
| `users.status` | `disabled` | 禁用用户。 |
| `users.status` | `deleted` | 已删除用户。 |
| `membership_plans.duration_unit` | `day` | 按天计算。 |
| `membership_plans.duration_unit` | `month` | 按月计算。 |
| `membership_plans.duration_unit` | `custom` | 自定义天数。 |
| `membership_plans.status` | `active` | 启用套餐。 |
| `membership_plans.status` | `inactive` | 停用套餐。 |
| `user_memberships.source` | `payment` | 支付开通。 |
| `user_memberships.source` | `admin_adjustment` | 后台手动调整。 |
| `user_memberships.status` | `active` | 会员有效。 |
| `user_memberships.status` | `expired` | 会员过期。 |
| `user_memberships.status` | `cancelled` | 会员取消。 |

## 支付

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `payment_orders.provider` | `wechat_pay` | 微信支付。 |
| `payment_orders.provider` | `alipay` | 支付宝。 |
| `payment_orders.provider` | `offline` | 线下支付，由后台登记确认。 |
| `payment_notifications.provider` | `wechat_pay` | 微信支付回调。 |
| `payment_notifications.provider` | `alipay` | 支付宝回调。 |
| `payment_orders.client_type` | `web` | 学生 Web。 |
| `payment_orders.client_type` | `mobile` | 手机端。 |
| `payment_orders.client_type` | `admin` | 管理后台。 |
| `payment_orders.status` | `pending` | 待支付。 |
| `payment_orders.status` | `paid` | 已支付。 |
| `payment_orders.status` | `failed` | 支付失败或后台确认无法完成。 |
| `payment_orders.status` | `refunded` | 已退款。 |
| `payment_notifications.process_status` | `handled` | 回调已处理完成。 |
| `payment_notifications.process_status` | `ignored` | 回调有效但未触发业务状态变更，例如非支付成功状态。 |
| `payment_notifications.process_status` | `failed` | 回调处理失败，例如验签、订单、渠道或金额校验失败。 |

## 内容与媒体

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `media_assets.media_type` | `audio` | 音频。 |
| `media_assets.media_type` | `image` | 图片。 |
| `media_assets.media_type` | `video` | 视频。 |
| `media_assets.status` | `active` | 启用媒体资源。 |
| `media_assets.status` | `inactive` | 停用媒体资源。 |
| `language` | `zh` | 中文。 |
| `language` | `ru` | 俄语。 |
| `language` | `en` | 英语。 |
| `vocab_lists.list_type` | `HSK` | HSK 词表。 |
| `vocab_lists.list_type` | `YCT` | YCT 词表。 |
| `vocab_lists.list_type` | `category` | 分类词汇。 |
| `vocab_lists.list_type` | `professional` | 专业词汇。 |
| `vocab_lists.list_type` | `custom` | 自定义词表。 |
| `vocab_lists.status` | `active` | 启用词表。 |
| `vocab_lists.status` | `inactive` | 停用词表。 |
| `vocab_items.status` | `active` | 启用词汇。 |
| `vocab_items.status` | `inactive` | 停用词汇。 |
| `user_vocab_item_progress.status` | `learning` | 正在学习，尚未标记为已学。 |
| `user_vocab_item_progress.status` | `learned` | 已学过该词。 |
| `user_vocab_item_progress.status` | `reviewing` | 已学过，进入复习状态。 |
| `user_vocab_item_progress.status` | `mastered` | 已掌握该词。 |

## 练习与台词

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `exercise_sets.exercise_type` | `audio_order` | 听音频排序。 |
| `exercise_sets.exercise_type` | `audio_dictation` | 听写汉字。 |
| `exercise_sets.exercise_type` | `pinyin_dictation` | 看拼音写汉字。 |
| `exercise_sets.exercise_type` | `translation_order` | 按拼音排列句子（历史枚举名保留）。 |
| `sentence_exercises.exercise_type` | `audio_order` | 听音频排序。 |
| `sentence_exercises.exercise_type` | `audio_dictation` | 听写汉字。 |
| `sentence_exercises.exercise_type` | `pinyin_dictation` | 看拼音写汉字。 |
| `sentence_exercises.exercise_type` | `translation_order` | 按拼音排列句子（历史枚举名保留）。 |
| `exercise_attempts.exercise_type` | `audio_order` | 听音频排序作答。 |
| `exercise_attempts.exercise_type` | `audio_dictation` | 听写汉字作答。 |
| `exercise_attempts.exercise_type` | `pinyin_dictation` | 看拼音写汉字作答。 |
| `exercise_attempts.exercise_type` | `translation_order` | 按拼音排列句子作答（历史枚举名保留）。 |
| `exercise_sets.status` | `active` | 启用题组。 |
| `exercise_sets.status` | `inactive` | 停用题组。 |
| `sentence_exercises.status` | `active` | 启用题目。 |
| `sentence_exercises.status` | `inactive` | 停用题目。 |
| `user_sentence_progress.status` | `learning` | 已练习该句子，但尚未正确作答。 |
| `user_sentence_progress.status` | `learned` | 已正确作答过该句子。 |
| `user_sentence_progress.status` | `reviewing` | 已学过，进入复习状态。 |
| `user_sentence_progress.status` | `mastered` | 已掌握该句子。 |
| `exercise_attempts.translation_language` | `ru` | 俄语翻译。 |
| `exercise_attempts.translation_language` | `en` | 英语翻译。 |
| `video_materials.material_type` | `drama` | 剧集材料。 |
| `video_materials.material_type` | `short_video` | 短视频材料。 |
| `video_materials.material_type` | `cartoon` | 动画材料。 |
| `video_materials.status` | `active` | 启用材料。 |
| `video_materials.status` | `inactive` | 停用材料。 |
| `asr_jobs.status` | `pending` | 待处理。 |
| `asr_jobs.status` | `processing` | 处理中。 |
| `asr_jobs.status` | `succeeded` | 识别成功。 |
| `asr_jobs.status` | `failed` | 识别失败。 |
| `matching_game_sessions.source_type` | `vocab_list` | 来源为词汇表。 |
| `matching_game_sessions.source_type` | `favorites` | 来源为收藏词。 |
| `matching_game_sessions.meaning_language` | `ru` | 俄语释义。 |
| `matching_game_sessions.meaning_language` | `en` | 英语释义。 |
| `matching_game_sessions.difficulty` | `^[A-Za-z0-9_-]{1,30}$` | 小关编码，实际启用关卡由 `system_configs.config_key = matching.stages` 的 stage/levels 配置维护。 |
| `matching_game_sessions.game_type` | `matching` | 连连看。 |
| `matching_game_sessions.game_type` | `elimination` | 消消乐。 |
| `matching_game_sessions.status` | `playing` | 游戏进行中。 |
| `matching_game_sessions.status` | `completed` | 游戏完成。 |
| `matching_game_sessions.status` | `abandoned` | 游戏放弃。 |
| `matching_game_sessions.status` | `failed` | 限时结束或挑战失败。 |

## 班级

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `classes.status` | `active` | 正常班级。 |
| `classes.status` | `archived` | 已归档班级。 |
| `classes.status` | `deleted` | 已删除班级。 |
| `class_members.status` | `active` | 正式成员。 |
| `class_members.status` | `left` | 成员已退出。 |
| `class_members.status` | `removed` | 被移除。 |

## 学习统计

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `study_events.event_type` | `exercise` | 练习。 |
| `study_events.event_type` | `vocab` | 背单词。 |
| `study_events.event_type` | `dialogue` | 台词训练。 |
| `study_events.event_type` | `matching_game` | 连连看。 |
| `study_events.result` | `correct` | 正确。 |
| `study_events.result` | `wrong` | 错误。 |
| `study_events.result` | `completed` | 完成。 |
| `leaderboard_entries.period_type` | `daily` | 日榜。 |
| `leaderboard_entries.period_type` | `weekly` | 周榜。 |
| `leaderboard_entries.period_type` | `monthly` | 月榜。 |
| `leaderboard_entries.period_type` | `all` | 总榜。 |
| `leaderboard_entries.metric_type` | `streak` | 连续学习天数。 |
| `leaderboard_entries.metric_type` | `accuracy` | 正确率。 |
| `leaderboard_entries.metric_type` | `vocab_count` | 背词数量。 |
| `leaderboard_entries.metric_type` | `game_score` | 游戏分值。 |

## 管理后台

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `admin_users.status` | `active` | 启用管理员。 |
| `admin_users.status` | `disabled` | 禁用管理员。 |
| `system_configs.config_group` | `payment` | 支付配置。 |
| `system_configs.config_group` | `asr` | 本地语音识别配置。 |
| `system_configs.config_group` | `membership` | 会员配置。 |
| `system_configs.config_group` | `upload` | 上传配置。 |
| `system_configs.config_group` | `learning` | 学习玩法配置，例如连连看/消消乐关卡。 |
