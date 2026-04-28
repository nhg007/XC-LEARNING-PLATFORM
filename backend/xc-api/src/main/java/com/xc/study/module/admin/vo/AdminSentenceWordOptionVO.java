package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminSentenceWordOptionVO(
        Long id,
        Long exerciseId,
        String wordText,
        Integer correctOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
