package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpsertDialogueLineDTO(
        @NotNull Long materialId,
        @NotNull @Min(1) @Max(999999) Integer lineNo,
        @NotBlank @Size(max = 4000) String hanziText,
        @Size(max = 4000) String pinyinText,
        @Size(max = 4000) String translationEn,
        @Size(max = 4000) String translationRu,
        Long audioAssetId,
        @Min(0) Integer startMs,
        @Min(0) Integer endMs
) {
}
