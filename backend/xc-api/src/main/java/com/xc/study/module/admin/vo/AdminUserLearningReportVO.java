package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminUserLearningReportVO(
        Long userId,
        String email,
        String nickname,
        String status,
        int studySeconds,
        int exerciseCount,
        int correctCount,
        int vocabReviewCount,
        int dialogueCount,
        int matchingGameCount,
        BigDecimal accuracyRate,
        LocalDate lastStudyDate
) {
}
