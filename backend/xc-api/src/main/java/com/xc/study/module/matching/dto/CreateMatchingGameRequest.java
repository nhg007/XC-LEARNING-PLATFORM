package com.xc.study.module.matching.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateMatchingGameRequest(
        @Pattern(regexp = "matching|elimination") String gameType,
        @NotBlank @Pattern(regexp = "vocab_list|favorites") String sourceType,
        Long vocabListId,
        @NotBlank @Pattern(regexp = "ru|en") String meaningLanguage,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,30}") String difficulty
) {
}
