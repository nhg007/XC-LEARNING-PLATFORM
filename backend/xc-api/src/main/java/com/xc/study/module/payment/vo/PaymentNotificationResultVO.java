package com.xc.study.module.payment.vo;

public record PaymentNotificationResultVO(
        String orderNo,
        String status,
        boolean handled
) {
}
