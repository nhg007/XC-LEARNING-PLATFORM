package com.xc.study.module.dialogue.vo;

import java.util.List;

public record DialogueLineVO(
        Long id,
        Long materialId,
        Integer lineNo,
        String hanziText,
        String pinyinText,
        String translationEn,
        String translationRu,
        Long audioAssetId,
        String audioUrl,
        Integer startMs,
        Integer endMs,
        List<String> wordOptions
) {
}
