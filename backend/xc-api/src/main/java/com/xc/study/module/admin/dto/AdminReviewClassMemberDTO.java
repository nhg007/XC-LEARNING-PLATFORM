package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReviewClassMemberDTO(
        @NotNull Boolean approved,
        @Size(max = 1000) String reason
) {
}
