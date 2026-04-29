# XC Chinese Learning Platform

汉语学习网站工程仓库，基于 `Study/基本设计.md` 创建。

## 工程分层

| 目录 | 职责 |
| --- | --- |
| `backend/xc-api` | Spring Boot 3 后端 API，负责账号、会员、支付、班级、内容、学习统计和后台接口。 |
| `frontend/xc-admin` | 管理后台，vue-pure-admin 精简版（国际化版）+ Vue3 + TypeScript，负责用户、会员、订单、班级、内容和报表管理。 |
| `frontend/xc-web` | 学生 Web 前台，Vue3 + Vuetify 3 + TypeScript + vue-i18n，负责学习、会员、背词、班级和学习记录。 |
| `mobile/xc-uniapp` | 手机端 UniApp 工程，首期先保留最小 H5/小程序入口。 |
| `database/migrations` | PostgreSQL 数据库迁移脚本。 |
| `database/ENUMS.md` | 数据库状态值和枚举值集中说明。 |
| `deploy/docker` | 本地开发依赖服务 Docker Compose。 |
| `deploy/nginx` | Nginx 反向代理配置示例。 |
| `docs` | 工程说明、模块职责和后续开发记录。 |

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis Plus、PostgreSQL、Flyway、Redis、MinIO、JWT
- 管理后台：vue-pure-admin 精简版（国际化版）、Vue3、Vite、TypeScript、Element Plus、Pinia、Vue Router
- 学生 Web：Vue3、Vite、TypeScript、Vuetify 3、Pinia、Vue Router、vue-i18n
- 手机端：UniApp、Vue3、TypeScript
- 部署：Docker Compose、Nginx

## 环境准备

当前工程不硬编码账号、密码、IP 或域名。请复制环境变量示例后按本机环境调整：

```bash
cp .env.example .env
cp backend/xc-api/.env.dev.example backend/xc-api/.env.dev
cp frontend/xc-admin/.env.dev.example frontend/xc-admin/.env.dev
cp frontend/xc-web/.env.dev.example frontend/xc-web/.env.dev
cp mobile/xc-uniapp/.env.dev.example mobile/xc-uniapp/.env.dev
```

所有工程配置按 `dev`、`stg`、`prod` 三套环境分层。后端使用 Spring profile：`application-dev.yml`、`application-stg.yml`、`application-prod.yml`；前端和 UniApp 使用 Vite mode：`.env.dev`、`.env.stg`、`.env.prod`。仓库只提交 `.example` 示例文件。

后端 `dev` 环境默认启用 Flyway，并从 `database/migrations` 执行数据库迁移；`stg`、`prod` 默认关闭启动时自动迁移，需要发布流程确认后通过 `DB_FLYWAY_ENABLED=true` 开启。

后端 `dev` 环境可通过 `APP_BOOTSTRAP_DEMO_CONTENT_ENABLED=true` 独立初始化演示内容，包括词表、词汇条目、句子题、台词材料和台词解析；它不会创建学生账号、老师账号、订单、会员或 MinIO 媒体文件。演示用户和线下支付闭环仍由 `APP_BOOTSTRAP_DEMO_FLOW_*` 控制，音频、图片、视频等真实文件仍通过内容管理上传或绑定。

Redis 缓存默认不强制开启。需要缓存读多写少的 master 数据时，在后端环境变量中设置 `APP_REDIS_CACHE_ENABLED=true`；当前缓存范围包括会员套餐、词表目录、词汇条目、题组目录、台词材料目录，以及句子答案、台词行和台词解析等纯内容详情。词汇条目缓存只保存 master 内容，用户收藏状态会在请求时实时合并。后台变更对应内容后会自动清理相关 key。订单、会员权益和学习记录仍以 PostgreSQL 为准。

需要安装：

- JDK 17+
- Maven 3.9+
- Node.js 20+
- pnpm 10+
- Docker / Docker Compose

## 启动方法

启动基础依赖：

```bash
cd deploy/docker
cp .env.dev.example .env.dev
docker compose --env-file .env.dev up -d
```

启动后端：

```bash
cd backend/xc-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

后端接口统一返回 `success`、`code`、`message`、`data`、`traceId`、`timestamp`，响应头会返回同一次请求的 `X-Request-Id`。

启动管理后台：

```bash
cd frontend/xc-admin
pnpm install
pnpm dev
```

启动学生 Web：

```bash
cd frontend/xc-web
pnpm install
pnpm dev
```

启动手机端 H5：

```bash
cd mobile/xc-uniapp
pnpm install
pnpm dev:h5
```

## 构建方法

```bash
cd backend/xc-api && mvn clean package -Pprod
cd frontend/xc-admin && pnpm build:prod
cd frontend/xc-web && pnpm build:prod
cd mobile/xc-uniapp && pnpm build:h5:prod
```

## 测试方法

```bash
cd backend/xc-api && mvn test
cd frontend/xc-admin && pnpm typecheck
cd frontend/xc-web && pnpm typecheck
```

## 后续开发入口

1. 开发环境启动后端时由 Flyway 执行 `database/migrations/V1__core_schema.sql` 创建 PostgreSQL 基础表。
2. 后端从 `backend/xc-api/src/main/java/com/xc/study/module` 按模块补 Entity、Mapper、Service、Controller。
3. 管理后台从 `frontend/xc-admin/src/views` 补用户、会员、订单、班级、词汇、题库、媒体和报表页面。
4. 学生 Web 从 `frontend/xc-web/src/views` 补学习入口、背单词、练习、连连看、台词训练和班级页面。
5. 手机端在 `mobile/xc-uniapp/src/pages` 中按移动端优先级补页面。
