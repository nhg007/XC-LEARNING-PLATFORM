package com.xc.study.module.stats.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LeaderboardEntryVO(
        Long id,
        String periodType,
        LocalDate periodStart,
        String metricType,
        Long userId,
        String nickname,
        BigDecimal scoreValue,
        Integer rankNo,
        OffsetDateTime generatedAt
) {
}
