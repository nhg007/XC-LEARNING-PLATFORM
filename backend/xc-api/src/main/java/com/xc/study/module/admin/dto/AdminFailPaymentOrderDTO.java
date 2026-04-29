package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminFailPaymentOrderDTO(
        @NotBlank @Size(max = 1000) String reason
) {
}
