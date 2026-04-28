package com.xc.study.module.payment.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentOrderVO(
        Long id,
        String orderNo,
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
        OffsetDateTime updatedAt,
        boolean mockPayment
) {
}
