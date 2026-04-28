package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminPaymentOrderVO(
        Long id,
        String orderNo,
        Long userId,
        String userEmail,
        String userNickname,
        Long planId,
        String planName,
        BigDecimal amount,
        String currency,
        String provider,
        String clientType,
        String paymentUrl,
        String providerTradeNo,
        String status,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
