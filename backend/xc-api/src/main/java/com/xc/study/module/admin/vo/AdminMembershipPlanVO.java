package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminMembershipPlanVO(
        Long id,
        String name,
        Integer durationDays,
        String durationUnit,
        Integer durationValue,
        BigDecimal price,
        String currency,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
