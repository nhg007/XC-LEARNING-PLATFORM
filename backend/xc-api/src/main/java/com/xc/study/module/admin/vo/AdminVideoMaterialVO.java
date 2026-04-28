package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminVideoMaterialVO(
        Long id,
        String title,
        String materialType,
        String description,
        Long coverAssetId,
        String coverUrl,
        String status,
        long lineCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
