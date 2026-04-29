# 本地 ASR 任务

台词跟读录音会先保存到媒体存储，再创建 `asr_jobs` 和 `speech_records`。后端定时 worker 领取 `pending` 任务，调用 ASR 适配器，成功后回写识别文本、对比结果和分数；失败时记录 `failed` 与错误信息，手机端通过 `/api/speech-records/{id}` 轮询状态。

## 配置

```properties
ASR_ENGINE_NAME=local-asr
ASR_PROVIDER=mock
ASR_WORKER_ENABLED=true
ASR_INITIAL_DELAY_MS=5000
ASR_POLL_DELAY_MS=5000
ASR_BATCH_SIZE=5
ASR_SERVICE_URL=
ASR_SERVICE_PATH=/recognize
ASR_MOCK_RECOGNIZED_TEXT=
```

- `ASR_PROVIDER=mock`：开发环境跑通流程，默认返回标准台词；如配置 `ASR_MOCK_RECOGNIZED_TEXT`，则返回该固定文本。
- `ASR_PROVIDER=http`：调用内网本地 ASR 服务，使用 `multipart/form-data` 上传 `file`，并携带 `jobId`、`engineName`、`expectedText`。
- HTTP ASR 服务返回 JSON 时支持 `recognizedText`、`text`，或 `data.recognizedText`、`data.text`。

`stg`、`prod` 建议使用 `ASR_PROVIDER=http`，部署完成并确认服务地址后再开启 `ASR_WORKER_ENABLED=true`。
