package com.xc.study.module.vocab.vo;

import java.time.OffsetDateTime;

public record VocabItemVO(
        Long id,
        Long vocabListId,
        String hanzi,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String exampleSentence,
        Long audioAssetId,
        String audioUrl,
        Integer sortOrder,
        boolean favorite,
        String progressStatus,
        Integer reviewCount,
        OffsetDateTime learnedAt,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime nextReviewAt
) {
}
