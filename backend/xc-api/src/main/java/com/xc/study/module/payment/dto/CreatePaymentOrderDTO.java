package com.xc.study.module.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreatePaymentOrderDTO(
        @NotNull Long planId,
        @NotBlank @Pattern(regexp = "wechat_pay|alipay") String provider,
        @Pattern(regexp = "web|mobile|admin") String clientType
) {
}
