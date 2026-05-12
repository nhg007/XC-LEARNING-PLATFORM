package com.xc.study.module.classroom.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ClassMemberStatsVO(
        Long userId,
        String email,
        String nickname,
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
