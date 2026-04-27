package com.xc.study.module.exercise.vo;

public record ExerciseAnswerVO(
        Long exerciseId,
        String hanziAnswer,
        String explanation,
        String translationEn,
        String translationRu
) {
}
