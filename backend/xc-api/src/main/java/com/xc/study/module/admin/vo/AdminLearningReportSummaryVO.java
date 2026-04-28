package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminLearningReportSummaryVO(
        LocalDate dateFrom,
        LocalDate dateTo,
        int activeUserCount,
        int studySeconds,
        int exerciseCount,
        int correctCount,
        int vocabReviewCount,
        int dialogueCount,
        int matchingGameCount,
        BigDecimal accuracyRate
) {
}
