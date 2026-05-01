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

## 本番 Docker 部署

本番环境推荐使用 `deploy/docker/docker-compose.prod.yml` 启动完整栈：PostgreSQL、Redis、MinIO、后端 API 和 Nginx。Nginx 使用单域名路径模式：

| 路径 | 服务 |
| --- | --- |
| `/` | 学生 Web |
| `/admin/` | 管理后台 |
| `/h5/` | 手机 H5 |
| `/api/` | 后端 API |

Docker 本番部署只需要维护一份运行时环境变量文件：`deploy/docker/.env.prod`。首次部署前复制示例并填入真实域名、密码和密钥：

```bash
cp deploy/docker/.env.prod.example deploy/docker/.env.prod
cp frontend/xc-admin/.env.prod.example frontend/xc-admin/.env.prod
cp frontend/xc-web/.env.prod.example frontend/xc-web/.env.prod
cp mobile/xc-uniapp/.env.prod.example mobile/xc-uniapp/.env.prod
```

生产 Docker Compose 的后端、数据库、Redis、MinIO 都读取 `deploy/docker/.env.prod`。其中 `JWT_SECRET`、`POSTGRES_PASSWORD`、`DB_PASSWORD`、`REDIS_PASSWORD`、`MINIO_ROOT_PASSWORD`、支付回调密钥等必须换成部署平台生成的强随机值，不能使用示例值。

`backend/xc-api/.env.prod` 不参与 Docker 本番部署，通常不要在生产服务器上额外创建它。它只用于不走 Docker、直接执行 `java -jar` 启动后端，或本地临时用 `prod` profile 调试后端。避免同时维护两份后端 prod 配置，否则很容易出现密码、域名或开关不一致。

前端三个 `.env.prod` 文件只在构建静态资源时使用，用于写入 `VITE_API_BASE_URL` 和访问路径：

| 文件 | 用途 |
| --- | --- |
| `frontend/xc-admin/.env.prod` | 管理后台构建配置，默认 `/admin/` |
| `frontend/xc-web/.env.prod` | 学生 Web 构建配置，默认 `/` |
| `mobile/xc-uniapp/.env.prod` | H5 构建配置，默认 `/h5/` |

空库首次启动需要初始化一个系统管理员时，在 `deploy/docker/.env.prod` 中临时开启：

```env
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_USERNAME=你的管理员登录账号
APP_BOOTSTRAP_ADMIN_PASSWORD=至少12位的强密码
APP_BOOTSTRAP_ADMIN_DISPLAY_NAME=System Admin
```

启动后系统会在账号不存在时创建该管理员，并授予 `super_admin` 角色；如果账号已存在，只会补齐超级管理员权限，不会覆盖密码。首次登录确认正常后，建议把 `APP_BOOTSTRAP_ADMIN_ENABLED` 改回 `false` 并重启 API。

注意：`APP_BOOTSTRAP_ADMIN_PASSWORD` 必须至少 12 位；长度不足时系统会跳过管理员初始化，并在 API 日志中输出 `Admin bootstrap password must be at least 12 characters.`。如果 `APP_BOOTSTRAP_ADMIN_USERNAME` 对应账号已经存在，修改 `.env.prod` 中的 bootstrap 密码不会重置该账号密码，需要登录后台后通过系统管理重置，或按本番应急流程在数据库中重置密码。

构建并启动：

```bash
cd backend/xc-api && mvn clean package -Pprod
cd ../../frontend/xc-admin && pnpm build:prod
cd ../xc-web && pnpm build:prod
cd ../../mobile/xc-uniapp && pnpm build:h5:prod

cd ../../deploy/docker
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

本番默认不开放 PostgreSQL 和 Redis 端口；MinIO 仅绑定服务器本机 `127.0.0.1` 便于运维。对外入口只暴露 Nginx 的 `NGINX_HTTP_PORT`。如需 HTTPS，建议在云负载均衡、Caddy、Traefik 或宿主机 Nginx 层处理 TLS 后再转发到本 compose。

生产数据库迁移默认关闭：`DB_FLYWAY_ENABLED=false`。首次建库或发版需要迁移时，先备份数据库，再临时设置 `DB_FLYWAY_ENABLED=true` 启动一次；迁移成功后建议改回 `false`。

如果本番出现 `ERROR: relation "admin_users" does not exist`，说明 `database/migrations/V1__core_schema.sql` 还没有在当前连接的数据库中成功执行，或后端连接到了错误的数据库。先确认 `deploy/docker/.env.prod` 中 `DB_HOST`、`DB_NAME`、`DB_USER` 和 PostgreSQL 容器的 `POSTGRES_DB`、`POSTGRES_USER` 是否对应同一个库，再查看迁移状态：

```bash
cd deploy/docker
docker compose --env-file .env.prod -f docker-compose.prod.yml exec postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "\dt"'
docker compose --env-file .env.prod -f docker-compose.prod.yml exec postgres sh -lc 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "select installed_rank, version, description, success from flyway_schema_history order by installed_rank"'
```

空库首次迁移时，先做数据库备份，再把 `deploy/docker/.env.prod` 里的 `DB_FLYWAY_ENABLED` 临时改成 `true` 并重启 API：

```bash
cd deploy/docker
docker compose --env-file .env.prod -f docker-compose.prod.yml exec postgres sh -lc 'pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > /tmp/xc_learning_before_migrate.sql'
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d xc-api
```

迁移成功并确认后台登录正常后，把 `DB_FLYWAY_ENABLED` 改回 `false`，再执行一次 `docker compose --env-file .env.prod -f docker-compose.prod.yml up -d xc-api` 固化运行配置。

本番后端错误日志默认输出到 Docker stdout/stderr，不会写到项目目录里的固定文件。排查 API 错误优先查看：

```bash
cd deploy/docker
docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=300 xc-api
docker logs --tail=300 xc-prod-api
docker inspect -f '{{.LogPath}}' xc-prod-api
```

最后一条命令会输出宿主机上的实际日志文件路径，默认 Docker json-file 驱动下一般形如 `/var/lib/docker/containers/<container-id>/<container-id>-json.log`。Nginx 入口日志同样通过 `docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=300 nginx` 查看。

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
