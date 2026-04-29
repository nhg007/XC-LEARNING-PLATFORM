package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminPaymentNotificationVO(
        Long id,
        Long orderId,
        String orderNo,
        Long userId,
        String userEmail,
        String userNickname,
        Long planId,
        String planName,
        BigDecimal orderAmount,
        String currency,
        String orderStatus,
        String provider,
        String providerTradeNo,
        Boolean signatureValid,
        Boolean handled,
        String processStatus,
        String resultCode,
        String resultMessage,
        String notifyPayload,
        OffsetDateTime receivedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
