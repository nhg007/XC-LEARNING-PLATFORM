package com.xc.study.module.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateMatchingGameRequest(
        @NotBlank @Pattern(regexp = "vocab_list|favorites") String sourceType,
        Long vocabListId,
        @NotBlank @Pattern(regexp = "ru|en") String meaningLanguage,
        @NotBlank @Pattern(regexp = "4x4|7x7|10x10") String difficulty
) {
}
