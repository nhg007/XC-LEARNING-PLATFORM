package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminVideoMaterialVO(
        Long id,
        String title,
        Long parentId,
        String parentTitle,
        String materialType,
        String description,
        Long coverAssetId,
        String coverUrl,
        String status,
        long childCount,
        long lineCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
