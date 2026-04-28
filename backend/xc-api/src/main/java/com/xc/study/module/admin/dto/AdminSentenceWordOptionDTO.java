package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminSentenceWordOptionDTO(
        @NotBlank @Size(max = 100) String wordText,
        @NotNull @Min(1) @Max(999999) Integer correctOrder
) {
}
