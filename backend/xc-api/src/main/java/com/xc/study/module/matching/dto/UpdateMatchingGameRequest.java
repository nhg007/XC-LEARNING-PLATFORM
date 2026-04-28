package com.xc.study.module.matching.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateMatchingGameRequest(
        @NotNull @Min(0) @Max(100) Integer matchedPairs,
        @NotNull @Min(0) @Max(10000) Integer wrongCount,
        @NotNull @Min(0) @Max(86400) Integer elapsedSeconds,
        @NotBlank @Pattern(regexp = "playing|completed|abandoned") String status
) {
}
