# 局域网 IP 访问

本地开发时，后端和前端都不要写死当前机器 IP。服务监听 `0.0.0.0`，浏览器使用同源 `/api` 访问，再由 Vite dev server 代理到本机后端。

## 端口

- 后端 API：`http://<本机局域网IP>:8080/api`
- 管理后台：`http://<本机局域网IP>:8091`
- 学生 Web：`http://<本机局域网IP>:9091`
- UniApp H5：`http://<本机局域网IP>:9092`

## 关键配置

后端：

```env
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
CORS_ALLOWED_ORIGIN_PATTERNS=*
```

前端和 UniApp：

```env
VITE_DEV_HOST=0.0.0.0
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
```

UniApp 真机、App 或小程序调试如果不经过 H5 dev server 代理，媒体图片/GIF 需要使用可从设备访问的绝对 API 地址：

```env
VITE_API_PUBLIC_BASE_URL=http://<本机局域网IP>:8080/api
```

## 启动顺序

1. 启动数据库、Redis、MinIO。
2. 启动后端。
3. 启动需要访问的前端项目。
4. 用当前机器的局域网 IP 加对应端口访问。

macOS 可用下面命令查看常见 Wi-Fi IP：

```bash
ipconfig getifaddr en0
```

如果别人能打开页面但登录失败，优先检查：

- 后端是否正在运行。
- 前端 `.env.dev` 是否仍然使用 `VITE_API_BASE_URL=/api`。
- UniApp 真机是否为 `VITE_API_PUBLIC_BASE_URL` 配置了设备可访问的后端地址。
- 访问方和开发机是否在同一局域网。
- macOS 防火墙是否允许 Node/Java 接收入站连接。
