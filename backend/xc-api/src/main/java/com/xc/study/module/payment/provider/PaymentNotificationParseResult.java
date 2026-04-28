package com.xc.study.module.payment.provider;

import java.math.BigDecimal;

public record PaymentNotificationParseResult(
        boolean signatureValid,
        boolean paid,
        String orderNo,
        String providerTradeNo,
        BigDecimal amount,
        String status
) {
}
