package com.xc.study.module.exercise.vo;

import java.time.OffsetDateTime;

public record FavoriteSentenceExerciseVO(
        Long id,
        Long exerciseSetId,
        String exerciseSetTitle,
        String exerciseType,
        String pinyinPrompt,
        String hanziAnswer,
        String translationEn,
        String translationRu,
        Long audioZhAssetId,
        String audioUrl,
        Integer sortOrder,
        String progressStatus,
        Integer attemptCount,
        Integer correctCount,
        OffsetDateTime learnedAt,
        OffsetDateTime lastPracticedAt,
        OffsetDateTime lastCorrectAt,
        OffsetDateTime nextReviewAt,
        boolean favorite
) {
}
