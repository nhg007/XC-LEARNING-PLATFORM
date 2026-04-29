package com.xc.study.module.dialogue.asr;

public record AsrRecognitionRequest(
        Long jobId,
        Long userId,
        Long audioAssetId,
        String objectKey,
        String contentType,
        long contentLength,
        byte[] audioBytes,
        Long dialogueLineId,
        String expectedText,
        String engineName
) {
}
