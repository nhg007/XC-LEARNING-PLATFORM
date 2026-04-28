package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminUserListItemVO(
        Long id,
        String email,
        String nickname,
        String status,
        String accessLevel,
        boolean fullAccess,
        OffsetDateTime trialEndsAt,
        OffsetDateTime membershipEndsAt,
        Integer totalStudySeconds,
        Integer totalExerciseCount,
        BigDecimal overallAccuracyRate,
        Integer currentStreakDays,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt
) {
}
