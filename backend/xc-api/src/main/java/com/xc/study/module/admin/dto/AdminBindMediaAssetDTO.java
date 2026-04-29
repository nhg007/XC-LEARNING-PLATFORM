package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminBindMediaAssetDTO(
        @NotNull Long targetId,
        @NotNull Long mediaAssetId
) {
}
