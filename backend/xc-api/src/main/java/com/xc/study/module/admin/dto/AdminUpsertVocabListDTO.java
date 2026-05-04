package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpsertVocabListDTO(
        @NotBlank @Size(max = 100) String name,
        Long parentId,
        @NotBlank @Pattern(regexp = "HSK|YCT|category|professional|custom") String listType,
        @Size(max = 20) String level,
        @Size(max = 2000) String description,
        @NotNull @Min(0) @Max(999999) Integer sortOrder,
        @Pattern(regexp = "active|inactive") String status
) {
}
