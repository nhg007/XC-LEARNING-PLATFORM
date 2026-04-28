# 媒体存储

后台上传的音频、图片、视频统一登记到 `media_assets`，业务表只保存 `audio_asset_id`、`cover_asset_id` 等外键。

## 存储模式

- `MEDIA_STORAGE_TYPE=local`：文件写入 `MEDIA_STORAGE_ROOT`，主要用于临时本地调试。
- `MEDIA_STORAGE_TYPE=minio`：文件写入 MinIO/S3 兼容 bucket，推荐用于 `dev`、`stg`、`prod`。

上传后的 `media_assets.url` 默认使用 `MEDIA_PUBLIC_URL_PREFIX=/api/media` 生成，例如 `/api/media/audio/xxx.mp3`。后端会通过这个地址从当前存储后端读取文件，因此 MinIO bucket 可以保持私有。若后续改成 CDN 或对象存储公网地址，可以把 `MEDIA_PUBLIC_URL_PREFIX` 配成对应公开前缀，并同步确认 bucket/CDN 访问策略。

## MinIO 配置

```properties
MEDIA_STORAGE_TYPE=minio
MEDIA_PUBLIC_URL_PREFIX=/api/media
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=change_me
MINIO_SECRET_KEY=change_me
MINIO_BUCKET=xc-learning
MINIO_CREATE_BUCKET=true
```

`dev` 可以允许应用自动创建 bucket；`stg`、`prod` 建议提前创建 bucket，并设置 `MINIO_CREATE_BUCKET=false`。
