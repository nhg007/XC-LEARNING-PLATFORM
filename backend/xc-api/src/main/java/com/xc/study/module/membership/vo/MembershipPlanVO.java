package com.xc.study.module.membership.vo;

import java.math.BigDecimal;

public record MembershipPlanVO(
        Long id,
        String name,
        Integer durationDays,
        String durationUnit,
        Integer durationValue,
        BigDecimal price,
        String currency
) {
}
