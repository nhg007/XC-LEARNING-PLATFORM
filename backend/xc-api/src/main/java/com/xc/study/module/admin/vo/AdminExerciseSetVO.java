package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminExerciseSetVO(
        Long id,
        String title,
        String exerciseType,
        String level,
        String status,
        long activeExerciseCount,
        long inactiveExerciseCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
