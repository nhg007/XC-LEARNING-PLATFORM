package com.xc.study.module.admin.vo;

public record AdminOrderExceptionSummaryVO(
        long allExceptions,
        long pendingTimeout,
        long callbackFailed,
        long amountMismatch,
        long providerMismatch,
        long membershipMissing
) {
}
