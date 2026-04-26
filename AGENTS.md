# AGENTS.md

## 项目约定

- 本项目根据 `Study/基本设计.md` 实现，需求变更先更新设计文档或在 `docs/` 中记录。
- 不硬编码账号、密码、IP、域名、密钥和支付参数；统一通过环境变量注入。
- 后端使用 Spring Boot 3 + MyBatis Plus + PostgreSQL。
- 管理后台和学生 Web 使用 Vue3 + Vite + TypeScript + Element Plus。
- 手机端使用 UniApp，首期优先 H5，后续按小程序/App 目标适配。
- 所有项目配置必须区分 `dev`、`stg`、`prod` 三套环境。

## 后端规范

- Spring Boot 配置使用 profile 分层：`application-dev.yml`、`application-stg.yml`、`application-prod.yml`。
- 后端 PostgreSQL 连接池统一使用 HikariCP，并在 `dev/stg/prod` 中分别配置连接池参数。
- 业务代码不允许手动创建数据库连接，统一通过 Spring DataSource/MyBatis Plus 管理。
- 每张业务表建议对应 `Entity`、`Mapper`、`Service`、`ServiceImpl`。
- 通用 CRUD 使用 MyBatis Plus；复杂统计、班级报表、排行榜使用自定义 Mapper SQL。
- Controller 返回统一响应结构 `ApiResponse<T>`。
- 所有受限接口必须在后端校验权限，不能只依赖前端隐藏菜单。
- 后端必须使用 `@RestControllerAdvice` 做全局异常处理，Controller/Service 不要为了返回错误响应而写大量局部 `try/catch`。
- 业务错误统一抛出 `BusinessException`；只有补偿、资源释放、重试、追加上下文或转换第三方异常时才局部捕获。
- 未预期异常必须记录日志并返回通用错误信息，不能向前端暴露堆栈、SQL、密钥或内部路径。
- 后台管理端和学生端统一使用 JWT 认证；登录成功后后端签发 Token，前端通过 `Authorization: Bearer <token>` 携带。
- JWT 密钥、过期时间、签发方等配置必须通过环境变量注入，禁止写死在代码或配置仓库中。
- 后台接口必须校验 JWT、管理员状态、角色和接口权限；管理员禁用、强制下线等场景后续可结合 Redis/Token 黑名单处理。
- 支付回调、会员手动调整、后台操作必须记录日志。

## 前端规范

- 前端配置使用 Vite mode 分层：`.env.dev`、`.env.stg`、`.env.prod`，仓库只提交对应 `.example` 文件。
- 页面按业务模块放入 `src/views`。
- API 调用统一放入 `src/api`。
- 路由统一在 `src/router` 管理，后续接入动态菜单权限。
- 状态管理使用 Pinia。
- 管理后台登录后保存 JWT，所有后台 API 请求必须自动附加 `Authorization: Bearer <token>`；退出登录必须清理 Token。
- 表格、表单、弹窗优先使用 Element Plus 原生组件，避免引入大型 UI 依赖。

## 数据库规范

- 数据库使用 PostgreSQL。
- 迁移脚本放入 `database/migrations`。
- 表名使用小写蛇形命名。
- 所有业务表保留 `created_at`、`updated_at`，状态字段使用明确枚举文本。
- DDL 必须包含主键、索引、字段注释。
- 外键关系必须创建 FK，不能只靠字段名或文档约定。
- 状态值、枚举值必须单独整理到 `database/ENUMS.md`，并在 DDL 中尽量使用 `check` 约束保护。

## 提交前检查

- 后端：`mvn test`
- 管理后台：`pnpm typecheck && pnpm build`
- 学生 Web：`pnpm typecheck && pnpm build`
- 确认 `.env`、密钥、真实支付参数没有提交。
