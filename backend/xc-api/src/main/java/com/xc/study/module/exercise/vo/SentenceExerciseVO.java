package com.xc.study.module.exercise.vo;

import java.util.List;

public record SentenceExerciseVO(
        Long id,
        Long exerciseSetId,
        String exerciseType,
        String pinyinPrompt,
        String translationEn,
        String translationRu,
        Long audioZhAssetId,
        Integer sortOrder,
        List<SentenceWordOptionVO> wordOptions
) {
}
