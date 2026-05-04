package com.xc.study.module.exercise.vo;

public record ExerciseSetVO(
        Long id,
        String title,
        Long parentId,
        String parentTitle,
        String exerciseType,
        String level,
        long childCount,
        long activeQuestionCount,
        long totalActiveQuestionCount
) {
}
