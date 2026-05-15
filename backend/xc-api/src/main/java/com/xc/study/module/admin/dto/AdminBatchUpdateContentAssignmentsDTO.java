package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminBatchUpdateContentAssignmentsDTO(
        @NotEmpty @Size(max = 200) List<@NotNull @Positive Long> ids,
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> targetIds
) {
}
