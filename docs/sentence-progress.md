# 句子学习状态设计

## 目标

- `exercise_attempts` 继续保存每次作答流水，用于统计正确率和学习记录。
- `user_sentence_progress` 保存用户对某个句子的当前学习状态，用于页面展示和后续复习队列。
- 没有进度记录的句子显示为“未学”。

## 状态

`user_sentence_progress.status` 使用以下状态：

| 状态 | 含义 |
| --- | --- |
| `learning` | 已练习该句子，但还没有正确作答。 |
| `learned` | 至少正确作答过一次。 |
| `reviewing` | 已进入复习队列。 |
| `mastered` | 已掌握。 |

## 写入规则

1. 学生提交句子答案时，后端先写入一条 `exercise_attempts` 作答记录。
2. 随后按 `user_id + sentence_exercise_id` 创建或更新 `user_sentence_progress`。
3. 每次提交都会增加 `attempt_count` 并更新 `last_practiced_at`。
4. 答对时增加 `correct_count`，更新 `last_correct_at`；首次答对时写入 `learned_at` 并将状态推进到 `learned`。
5. 答错时只创建或保持 `learning`，不会把 `learned`、`reviewing`、`mastered` 降级。
6. 后续复习算法可以使用 `reviewing`、`mastered` 和 `next_review_at`，不需要从历史作答流水反推当前状态。

## 前端展示

- 句子练习题目列表接口会返回每道题的 `progressStatus`、答题次数、正确次数和相关时间。
- Web 与 UniApp 练习页在题目 meta 区显示“未学 / 学习中 / 已学 / 复习中 / 已掌握”。
- 提交答案成功后，当前题目的状态立即用校验结果刷新，不需要重新进入题组。
