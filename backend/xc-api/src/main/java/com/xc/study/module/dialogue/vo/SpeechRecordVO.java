package com.xc.study.module.dialogue.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SpeechRecordVO(
        Long id,
        Long dialogueLineId,
        Long audioAssetId,
        String audioUrl,
        Long asrJobId,
        String asrStatus,
        String recognizedText,
        String compareResult,
        BigDecimal score,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
