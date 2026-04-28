package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminDialogueLineVocabVO(
        Long id,
        Long dialogueLineId,
        Long materialId,
        String materialTitle,
        Integer lineNo,
        String lineHanziText,
        Long vocabItemId,
        String vocabItemHanzi,
        String wordText,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String explanation,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
