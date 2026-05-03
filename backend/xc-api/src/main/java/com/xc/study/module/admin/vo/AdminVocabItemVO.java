package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminVocabItemVO(
        Long id,
        Long vocabListId,
        String vocabListName,
        String listType,
        String level,
        String vocabListStatus,
        String hanzi,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String exampleSentence,
        Long audioAssetId,
        String audioUrl,
        Integer sortOrder,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
