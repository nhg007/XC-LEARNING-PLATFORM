package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminClassMemberStatsVO(
        Long userId,
        String email,
        String nickname,
        String memberRole,
        Integer studySeconds,
        Integer exerciseCount,
        Integer correctCount,
        Integer vocabReviewCount,
        Integer dialogueCount,
        Integer matchingGameCount,
        BigDecimal accuracyRate,
        OffsetDateTime lastStudyAt
) {
}
