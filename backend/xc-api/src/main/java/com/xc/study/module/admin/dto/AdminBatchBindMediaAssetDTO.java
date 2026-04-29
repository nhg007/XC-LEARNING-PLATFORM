package com.xc.study.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminBatchBindMediaAssetDTO(
        @NotEmpty @Size(max = 200) @Valid List<AdminBindMediaAssetDTO> bindings
) {
}
