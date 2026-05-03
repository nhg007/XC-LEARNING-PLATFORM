package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminDialogueLineVO(
        Long id,
        Long materialId,
        String materialTitle,
        String materialStatus,
        Integer lineNo,
        String hanziText,
        String pinyinText,
        String translationEn,
        String translationRu,
        Long audioAssetId,
        String audioUrl,
        Integer startMs,
        Integer endMs,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
