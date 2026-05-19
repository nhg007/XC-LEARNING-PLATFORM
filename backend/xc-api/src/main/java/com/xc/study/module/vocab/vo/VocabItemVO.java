package com.xc.study.module.vocab.vo;

import java.time.OffsetDateTime;
import java.util.List;

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
        Long strokeOrderAssetId,
        String strokeOrderUrl,
        List<VocabStrokeOrderAssetVO> strokeOrderAssets,
        Integer sortOrder,
        boolean favorite,
        String progressStatus,
        Integer reviewCount,
        OffsetDateTime learnedAt,
        OffsetDateTime lastReviewedAt,
        OffsetDateTime nextReviewAt
) {
}
