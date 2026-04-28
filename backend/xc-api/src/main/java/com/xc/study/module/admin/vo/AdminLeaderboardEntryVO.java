package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AdminLeaderboardEntryVO(
        Long id,
        String periodType,
        LocalDate periodStart,
        String metricType,
        Long userId,
        String email,
        String nickname,
        BigDecimal scoreValue,
        Integer rankNo,
        OffsetDateTime generatedAt
) {
}
