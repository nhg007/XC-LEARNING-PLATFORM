package com.xc.study.module.user.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateUserPreferenceRequest(
        @Pattern(regexp = "zh|en|ru") String uiLanguage,
        @Pattern(regexp = "ru|en") String translationLanguage,
        @Pattern(regexp = "ru|en") String vocabMeaningLanguage,
        @Pattern(regexp = "ru|en") String matchingMeaningLanguage,
        Boolean soundEnabled
) {
}
