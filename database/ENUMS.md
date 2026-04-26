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
| `payment_orders.client_type` | `web` | 学生 Web。 |
| `payment_orders.client_type` | `mobile` | 手机端。 |
| `payment_orders.client_type` | `admin` | 管理后台。 |
| `payment_orders.status` | `pending` | 待支付。 |
| `payment_orders.status` | `paid` | 已支付。 |
| `payment_orders.status` | `failed` | 支付失败。 |
| `payment_orders.status` | `refunded` | 已退款。 |

## 内容与媒体

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `media_assets.media_type` | `audio` | 音频。 |
| `media_assets.media_type` | `image` | 图片。 |
| `media_assets.media_type` | `video` | 视频。 |
| `language` | `zh` | 中文。 |
| `language` | `ru` | 俄语。 |
| `language` | `en` | 英语。 |
| `vocab_lists.list_type` | `HSK` | HSK 词表。 |
| `vocab_lists.list_type` | `YCT` | YCT 词表。 |
| `vocab_lists.list_type` | `category` | 分类词汇。 |
| `vocab_lists.list_type` | `professional` | 专业词汇。 |
| `vocab_lists.list_type` | `custom` | 自定义词表。 |

## 班级

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `classes.status` | `active` | 正常班级。 |
| `classes.status` | `archived` | 已归档班级。 |
| `classes.status` | `deleted` | 已删除班级。 |
| `class_members.member_role` | `teacher` | 老师。 |
| `class_members.member_role` | `member` | 普通成员。 |
| `class_members.status` | `invited` | 已邀请。 |
| `class_members.status` | `pending_teacher_review` | 待老师审核。 |
| `class_members.status` | `active` | 正式成员。 |
| `class_members.status` | `rejected` | 老师已拒绝。 |
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

## 管理后台

| 字段 | 值 | 说明 |
| --- | --- | --- |
| `admin_users.status` | `active` | 启用管理员。 |
| `admin_users.status` | `disabled` | 禁用管理员。 |
| `system_configs.config_group` | `payment` | 支付配置。 |
| `system_configs.config_group` | `asr` | 本地语音识别配置。 |
| `system_configs.config_group` | `membership` | 会员配置。 |
| `system_configs.config_group` | `upload` | 上传配置。 |
