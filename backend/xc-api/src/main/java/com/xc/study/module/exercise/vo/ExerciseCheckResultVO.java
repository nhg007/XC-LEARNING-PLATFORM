package com.xc.study.module.exercise.vo;

public record ExerciseCheckResultVO(
        Long attemptId,
        Long exerciseId,
        boolean correct,
        String answerText,
        String standardAnswer,
        Integer firstMismatchIndex,
        String message
) {
}
