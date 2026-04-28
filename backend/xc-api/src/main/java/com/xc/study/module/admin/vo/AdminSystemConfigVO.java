package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminSystemConfigVO(
        Long id,
        String configKey,
        String configValue,
        String configGroup,
        String description,
        Long updatedBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
