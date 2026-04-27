package com.xc.study.module.stats.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStatVO(
        LocalDate statDate,
        Integer studySeconds,
        Integer exerciseCount,
        Integer correctCount,
        Integer vocabReviewCount,
        Integer dialogueCount,
        Integer matchingGameCount,
        BigDecimal accuracyRate
) {
}
