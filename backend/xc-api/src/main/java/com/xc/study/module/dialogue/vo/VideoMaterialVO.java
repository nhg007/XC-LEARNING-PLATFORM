package com.xc.study.module.dialogue.vo;

public record VideoMaterialVO(
        Long id,
        String title,
        Long parentId,
        String parentTitle,
        String materialType,
        String description,
        Long coverAssetId,
        String coverUrl,
        long childCount,
        long lineCount,
        long totalLineCount
) {
}
