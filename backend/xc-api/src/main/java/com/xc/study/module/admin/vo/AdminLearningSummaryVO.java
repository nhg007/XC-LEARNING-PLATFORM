package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminLearningSummaryVO(
        Integer totalStudySeconds,
        Integer totalExerciseCount,
        Integer totalCorrectCount,
        Integer totalVocabReviewCount,
        Integer currentStreakDays,
        Integer longestStreakDays,
        BigDecimal overallAccuracyRate,
        LocalDate lastStudyDate
) {
}
