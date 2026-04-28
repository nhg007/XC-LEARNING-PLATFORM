package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateAccountDTO(
        @Size(max = 100) String displayName,
        @NotBlank @Pattern(regexp = "active|disabled") String status
) {
}
