package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminVocabListVO(
        Long id,
        String name,
        Long parentId,
        String parentName,
        String listType,
        String level,
        String description,
        Integer sortOrder,
        String status,
        long childCount,
        long activeItemCount,
        long inactiveItemCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
