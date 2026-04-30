package com.xc.study.module.dialogue.asr;

import com.xc.study.module.admin.service.RuntimeConfigService;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.asr", name = "provider", havingValue = "http")
public class HttpAsrClient implements AsrClient {

    private final AsrProperties properties;
    private final RuntimeConfigService runtimeConfigService;

    public HttpAsrClient(AsrProperties properties, RuntimeConfigService runtimeConfigService) {
        this.properties = properties;
        this.runtimeConfigService = runtimeConfigService;
    }

    @Override
    public AsrRecognitionResult recognize(AsrRecognitionRequest request) {
        String serviceUrl = runtimeConfigService.getString(RuntimeConfigService.ASR_SERVICE_URL, properties.getServiceUrl());
        String servicePath = runtimeConfigService.getString(RuntimeConfigService.ASR_SERVICE_PATH, properties.getServicePath());
        long timeoutMs = runtimeConfigService.getLong(RuntimeConfigService.ASR_TIMEOUT_MS, properties.getTimeoutMs());
        if (!StringUtils.hasText(serviceUrl)) {
            throw new AsrException("ASR_SERVICE_URL 未配置");
        }
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new AudioByteArrayResource(request.audioBytes(), request.objectKey()));
        body.add("jobId", String.valueOf(request.jobId()));
        body.add("engineName", request.engineName());
        body.add("expectedText", request.expectedText());

        AsrHttpResponse response;
        try {
            response = restClient(timeoutMs).post()
                    .uri(stripTrailingSlash(serviceUrl) + normalizePath(servicePath))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(AsrHttpResponse.class);
        } catch (RuntimeException ex) {
            throw new AsrException("ASR 服务调用失败", ex);
        }
        String recognizedText = resolveRecognizedText(response);
        if (!StringUtils.hasText(recognizedText)) {
            throw new AsrException("ASR 服务未返回识别文本");
        }
        return new AsrRecognitionResult(recognizedText.trim());
    }

    private RestClient restClient(long timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofMillis(Math.max(1000L, timeoutMs));
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private String resolveRecognizedText(AsrHttpResponse response) {
        if (response == null) {
            return "";
        }
        if (StringUtils.hasText(response.recognizedText())) {
            return response.recognizedText();
        }
        if (StringUtils.hasText(response.text())) {
            return response.text();
        }
        Object nested = response.data() == null ? null : response.data().get("recognizedText");
        if (nested == null && response.data() != null) {
            nested = response.data().get("text");
        }
        return nested == null ? "" : nested.toString();
    }

    private String stripTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/recognize";
        }
        String result = path.trim();
        return result.startsWith("/") ? result : "/" + result;
    }

    private record AsrHttpResponse(String recognizedText, String text, Map<String, Object> data) {
    }

    private static final class AudioByteArrayResource extends ByteArrayResource {

        private final String filename;

        private AudioByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = StringUtils.hasText(filename) ? filename : "speech-recording.wav";
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
