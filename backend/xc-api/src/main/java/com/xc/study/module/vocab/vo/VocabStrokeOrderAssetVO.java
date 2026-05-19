package com.xc.study.module.vocab.vo;

public record VocabStrokeOrderAssetVO(
        Long id,
        Long mediaAssetId,
        String title,
        String url,
        Integer sortOrder
) {
}
