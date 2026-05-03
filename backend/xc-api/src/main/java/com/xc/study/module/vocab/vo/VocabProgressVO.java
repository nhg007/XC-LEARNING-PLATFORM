package com.xc.study.module.vocab.vo;

public record VocabProgressVO(
        Long vocabListId,
        Integer currentIndex,
        Long lastVocabItemId,
        Integer reviewedCount,
        Integer learnedCount,
        Integer reviewingCount,
        Integer masteredCount,
        long totalCount
) {
}
