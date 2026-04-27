package com.xc.study.module.stats.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LearningSummaryVO(
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
