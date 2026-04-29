package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminMediaAssetVO(
        Long id,
        String mediaType,
        String url,
        String language,
        Integer durationMs,
        Long fileSize,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
