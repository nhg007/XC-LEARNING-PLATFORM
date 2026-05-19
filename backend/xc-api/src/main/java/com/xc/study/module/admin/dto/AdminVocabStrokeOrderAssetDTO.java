package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminVocabStrokeOrderAssetDTO(
        Long mediaAssetId,
        @Size(max = 100) String title,
        @Min(0) @Max(999999) Integer sortOrder
) {
}
