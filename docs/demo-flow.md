# 演示验收流程

本文档用于 dev 环境跑一套干净流程：

1. 清理演示用户数据，只保留超级管理员。
2. 初始化“教师后台管理人员”角色和老师后台账号。
3. 创建演示班级并关联老师后台账号，加入演示学员。
4. 为演示学员生成线下已支付订单并开通会员。
5. 用同一学生账号验证 Web 与 H5 学习入口。

## 1. 清理和初始化

在 `backend/xc-api/.env.dev` 中打开以下开关：

```properties
APP_BOOTSTRAP_DEMO_FLOW_ENABLED=true
APP_BOOTSTRAP_DEMO_FLOW_RESET_USERS=true
APP_BOOTSTRAP_DEMO_FLOW_STANDARD_ENABLED=true
APP_BOOTSTRAP_DEMO_CONTENT_ENABLED=true
APP_BOOTSTRAP_DEMO_FLOW_TEACHER_ADMIN_USERNAME=teacher@example.com
APP_BOOTSTRAP_DEMO_FLOW_TEACHER_ADMIN_PASSWORD=change_me_dev_only
APP_BOOTSTRAP_DEMO_FLOW_TEACHER_ADMIN_DISPLAY_NAME=TeacherAdmin
APP_BOOTSTRAP_DEMO_FLOW_STUDENT_EMAIL=flow.student@example.com
APP_BOOTSTRAP_DEMO_FLOW_STUDENT_PASSWORD=change_me_dev_only
APP_BOOTSTRAP_DEMO_FLOW_STUDENT_NICKNAME=FlowStudent
APP_BOOTSTRAP_DEMO_FLOW_CLASS_NAME=Demo HSK1 \u73ED\u7EA7
APP_BOOTSTRAP_DEMO_FLOW_CLASS_DESCRIPTION=\u6807\u51C6\u6F14\u793A\u73ED\u7EA7
APP_BOOTSTRAP_DEMO_FLOW_CLASS_INVITE_CODE=DEMO2026
APP_BOOTSTRAP_DEMO_FLOW_OFFLINE_TRADE_NO=OFFLINE-DEMO-FLOW-001
```

`.env` 通过 Spring Boot 的 properties loader 读取，中文建议写成 `\uXXXX` 转义，避免初始化时出现乱码。

如果当前数据库还没有超级管理员，同时打开已有超级管理员初始化：

```properties
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_USERNAME=admin
APP_BOOTSTRAP_ADMIN_PASSWORD=change_me_dev_only
APP_BOOTSTRAP_ADMIN_DISPLAY_NAME=LocalAdmin
```

重启后端。启动成功后，建议立刻把下面这个开关改回 `false`，避免下次启动再次清数据：

```properties
APP_BOOTSTRAP_DEMO_FLOW_RESET_USERS=false
```

清理范围：

- 删除所有学生用户和用户相关学习、班级、订单、会员、支付回调数据。
- 删除除超级管理员以外的后台管理员。
- 保留内容数据、媒体数据、会员套餐、后台权限和超级管理员。
- `APP_BOOTSTRAP_DEMO_CONTENT_ENABLED=true` 会独立初始化演示词表、词汇条目、句子题、台词材料和台词解析；不创建学生账号、老师账号、订单或 MinIO 媒体文件。
- `teacher_admin` 是后台角色，绑定用户查看、班级查看/维护、报表查看权限。
- 标准流程会创建老师后台账号，并把该账号绑定到 `teacher_admin` 角色。
- 标准流程不会创建老师前台用户；班级通过 `classes.teacher_admin_user_id` 关联老师后台账号。
- 标准流程会创建演示学员、演示班级、学生班级成员关系、线下已支付订单和会员记录。
- 权限分配由超级管理员或拥有 `admin:system:update` 的后台账号完成，老师账号自身不应拥有权限分配能力。

## 2. 验证账号

标准流程完成后，默认可用账号如下：

- 超级管理员：按 `APP_BOOTSTRAP_ADMIN_USERNAME` 和 `APP_BOOTSTRAP_ADMIN_PASSWORD`。
- 老师后台账号：`teacher@example.com` / `change_me_dev_only`，用于登录管理后台。
- 演示学员：`flow.student@example.com` / `change_me_dev_only`，用于登录学生 Web 和 UniApp H5。

## 3. 验证线下支付与会员

学生登录后查询会员状态：

```bash
API=http://localhost:8080/api

curl -s -X POST "$API/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "account": "flow.student@example.com",
    "password": "change_me_dev_only"
  }'
```

从返回结果中取出 `data.token`，作为 `STUDENT_TOKEN`。

```bash
curl -s "$API/membership/status" \
  -H "Authorization: Bearer $STUDENT_TOKEN"
```

期望结果：

- `accessType` 为 `member`。
- `fullAccess` 为 `true`。
- `membershipEndsAt` 有有效时间。

## 4. 验证班级

- 管理后台：用 `teacher@example.com` 登录，应能进入班级和报表相关页面。
- 学生 Web / UniApp H5：用 `flow.student@example.com` 登录，班级列表应能看到 `Demo HSK1 班级`。

## 5. 验证 Web 与 H5

使用同一个学生账号登录：

- 学生 Web：进入背单词、句子练习、台词训练、连连看等会员功能。
- UniApp H5：进入相同学习功能，确认不再被会员权限拦截。

如果仍被拦截，优先检查：

- 后端是否已经重启并执行最新代码。
- 线下订单是否为 `paid`。
- `/api/membership/status` 是否返回 `fullAccess=true`。
- 前端/H5 的 `VITE_API_BASE_URL` 是否指向同一个后端。
