package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminUpsertMembershipPlanDTO(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "day|month|custom") String durationUnit,
        @NotNull @Min(1) @Max(3650) Integer durationValue,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @NotBlank @Size(max = 10) String currency,
        @Pattern(regexp = "active|inactive") String status
) {
}
