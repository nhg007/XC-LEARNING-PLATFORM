package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminVocabListVO(
        Long id,
        String name,
        String listType,
        String level,
        String description,
        Integer sortOrder,
        String status,
        long activeItemCount,
        long inactiveItemCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
