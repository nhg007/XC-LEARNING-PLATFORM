package com.xc.study.module.vocab.vo;

public record VocabItemVO(
        Long id,
        Long vocabListId,
        String hanzi,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String exampleSentence,
        Long audioAssetId,
        Integer sortOrder,
        boolean favorite
) {
}
