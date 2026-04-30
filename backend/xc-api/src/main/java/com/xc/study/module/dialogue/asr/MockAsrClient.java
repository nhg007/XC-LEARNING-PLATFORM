package com.xc.study.module.dialogue.asr;

import com.xc.study.module.admin.service.RuntimeConfigService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.asr", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAsrClient implements AsrClient {

    private final AsrProperties properties;
    private final RuntimeConfigService runtimeConfigService;

    public MockAsrClient(AsrProperties properties, RuntimeConfigService runtimeConfigService) {
        this.properties = properties;
        this.runtimeConfigService = runtimeConfigService;
    }

    @Override
    public AsrRecognitionResult recognize(AsrRecognitionRequest request) {
        String configuredText = runtimeConfigService.getString(
                RuntimeConfigService.ASR_MOCK_RECOGNIZED_TEXT,
                properties.getMockRecognizedText()
        );
        if (StringUtils.hasText(configuredText)) {
            return new AsrRecognitionResult(configuredText.trim());
        }
        return new AsrRecognitionResult(request.expectedText());
    }
}
