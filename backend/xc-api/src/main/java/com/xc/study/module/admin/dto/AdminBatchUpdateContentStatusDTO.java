package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminBatchUpdateContentStatusDTO(
        @NotEmpty @Size(max = 200) List<@NotNull @Positive Long> ids,
        @NotBlank @Pattern(regexp = "active|inactive") String status,
        @Size(max = 1000) String reason
) {
}
