package com.xc.study.module.vocab.vo;

public record VocabListVO(
        Long id,
        String name,
        Long parentId,
        String parentName,
        String listType,
        String level,
        String description,
        Integer sortOrder,
        long childCount,
        long activeItemCount,
        long totalActiveItemCount
) {
}
