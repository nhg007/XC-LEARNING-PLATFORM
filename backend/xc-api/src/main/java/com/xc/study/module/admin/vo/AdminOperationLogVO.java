package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminOperationLogVO(
        Long id,
        Long adminUserId,
        String action,
        String targetType,
        Long targetId,
        String detail,
        String ipAddress,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
