package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateUserStatusDTO(
        @NotBlank @Pattern(regexp = "active|disabled") String status,
        @Size(max = 1000) String reason
) {
}
