package com.xc.study.module.vocab.vo;

public record VocabProgressVO(
        Long vocabListId,
        Integer currentIndex,
        Long lastVocabItemId,
        Integer reviewedCount,
        long totalCount
) {
}
