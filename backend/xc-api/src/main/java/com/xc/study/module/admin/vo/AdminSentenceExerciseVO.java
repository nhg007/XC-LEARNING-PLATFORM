package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminSentenceExerciseVO(
        Long id,
        Long exerciseSetId,
        String exerciseSetTitle,
        String exerciseSetStatus,
        String exerciseType,
        String hanziAnswer,
        String pinyinPrompt,
        String translationEn,
        String translationRu,
        Long audioZhAssetId,
        String audioUrl,
        String explanation,
        Integer sortOrder,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AdminSentenceWordOptionVO> wordOptions
) {
}
