package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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
        List<String> exceptionTypes,
        String latestNotificationProcessStatus,
        String latestNotificationResultCode,
        String latestNotificationResultMessage,
        OffsetDateTime latestNotificationReceivedAt,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
