package com.xc.study.module.payment.provider;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.util.StringUtils;

final class PaymentPayloads {

    private PaymentPayloads() {
    }

    static String orderNo(Map<String, Object> payload) {
        return stringValue(payload.getOrDefault("orderNo", payload.get("out_trade_no")));
    }

    static String tradeNo(Map<String, Object> payload) {
        return stringValue(payload.getOrDefault("providerTradeNo", payload.get("trade_no")));
    }

    static String status(Map<String, Object> payload) {
        return stringValue(payload.getOrDefault("status", payload.get("trade_status")));
    }

    static BigDecimal amount(Map<String, Object> payload) {
        Object value = payload.getOrDefault("amount", payload.get("total_amount"));
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    static boolean paid(String status) {
        return !StringUtils.hasText(status)
                || "paid".equalsIgnoreCase(status)
                || "success".equalsIgnoreCase(status)
                || "TRADE_SUCCESS".equals(status)
                || "SUCCESS".equals(status);
    }

    static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
