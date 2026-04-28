package com.xc.study.module.payment.provider;

public record PaymentCreateResult(
        String paymentUrl,
        String providerTradeNo
) {
}
