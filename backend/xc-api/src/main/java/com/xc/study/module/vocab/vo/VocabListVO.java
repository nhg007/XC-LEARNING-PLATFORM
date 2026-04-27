package com.xc.study.module.vocab.vo;

public record VocabListVO(
        Long id,
        String name,
        String listType,
        String level,
        String description,
        Integer sortOrder
) {
}
