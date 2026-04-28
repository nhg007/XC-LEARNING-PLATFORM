package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AdminAdjustMembershipDTO(
        @NotBlank @Pattern(regexp = "grant|cancel") String action,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        @NotBlank @Size(max = 1000) String reason
) {
}
