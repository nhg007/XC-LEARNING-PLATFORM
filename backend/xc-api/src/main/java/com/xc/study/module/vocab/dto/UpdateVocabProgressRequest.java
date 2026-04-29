package com.xc.study.module.vocab.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record UpdateVocabProgressRequest(
        @NotNull @Min(0) Integer currentIndex,
        Long lastVocabItemId,
        @Min(0) Integer reviewedCount,
        @Min(0) @Max(86400) Integer durationSeconds
) {
}
