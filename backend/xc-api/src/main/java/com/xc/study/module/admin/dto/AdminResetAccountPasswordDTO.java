package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetAccountPasswordDTO(
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
