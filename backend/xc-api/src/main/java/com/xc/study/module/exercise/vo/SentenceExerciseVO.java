package com.xc.study.module.exercise.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record SentenceExerciseVO(
        Long id,
        Long exerciseSetId,
        String exerciseType,
        String pinyinPrompt,
        String translationEn,
        String translationRu,
        Long audioZhAssetId,
        String audioUrl,
        Integer sortOrder,
        List<SentenceWordOptionVO> wordOptions,
        String progressStatus,
        Integer attemptCount,
        Integer correctCount,
        OffsetDateTime learnedAt,
        OffsetDateTime lastPracticedAt,
        OffsetDateTime lastCorrectAt,
        OffsetDateTime nextReviewAt
) {
}
