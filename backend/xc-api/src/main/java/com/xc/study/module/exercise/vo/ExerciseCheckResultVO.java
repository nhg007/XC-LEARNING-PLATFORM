package com.xc.study.module.exercise.vo;

import java.time.OffsetDateTime;

public record ExerciseCheckResultVO(
        Long attemptId,
        Long exerciseId,
        boolean correct,
        String answerText,
        String standardAnswer,
        Integer firstMismatchIndex,
        String message,
        String progressStatus,
        Integer attemptCount,
        Integer correctCount,
        OffsetDateTime learnedAt,
        OffsetDateTime lastPracticedAt,
        OffsetDateTime lastCorrectAt,
        OffsetDateTime nextReviewAt
) {
}
