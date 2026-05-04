package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminExerciseSetVO(
        Long id,
        String title,
        Long parentId,
        String parentTitle,
        String exerciseType,
        String level,
        String status,
        long childCount,
        long activeExerciseCount,
        long inactiveExerciseCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
