package com.xc.study.module.user.vo;

public record UserPreferenceVO(
        String uiLanguage,
        String translationLanguage,
        String vocabMeaningLanguage,
        String matchingMeaningLanguage,
        Boolean soundEnabled
) {
}
