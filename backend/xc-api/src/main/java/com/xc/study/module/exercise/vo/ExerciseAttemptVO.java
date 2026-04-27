package com.xc.study.module.exercise.vo;

import java.time.OffsetDateTime;

public record ExerciseAttemptVO(
        Long id,
        Long exerciseId,
        String exerciseType,
        String answerText,
        boolean correct,
        boolean showedAnswer,
        String translationLanguage,
        OffsetDateTime answeredAt
) {
}
