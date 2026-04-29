package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminCreateOfflinePaymentOrderDTO(
        @NotBlank @Size(max = 128) String userKeyword,
        @NotNull Long planId,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @Size(max = 10) String currency,
        OffsetDateTime paidAt,
        @Size(max = 128) String offlineTradeNo,
        @NotBlank @Size(max = 1000) String remark
) {
}
