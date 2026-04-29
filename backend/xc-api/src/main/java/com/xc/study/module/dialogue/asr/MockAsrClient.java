package com.xc.study.module.dialogue.asr;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "app.asr", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAsrClient implements AsrClient {

    private final AsrProperties properties;

    public MockAsrClient(AsrProperties properties) {
        this.properties = properties;
    }

    @Override
    public AsrRecognitionResult recognize(AsrRecognitionRequest request) {
        String configuredText = properties.getMockRecognizedText();
        if (StringUtils.hasText(configuredText)) {
            return new AsrRecognitionResult(configuredText.trim());
        }
        return new AsrRecognitionResult(request.expectedText());
    }
}
