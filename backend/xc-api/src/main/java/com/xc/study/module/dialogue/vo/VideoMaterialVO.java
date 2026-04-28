package com.xc.study.module.dialogue.vo;

public record VideoMaterialVO(
        Long id,
        String title,
        String materialType,
        String description,
        Long coverAssetId,
        String coverUrl,
        long lineCount
) {
}
