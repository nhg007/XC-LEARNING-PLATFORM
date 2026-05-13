package com.xc.study.module.exercise.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckExerciseRequest(
        @Size(max = 4000) String answerText,
        @Size(max = 100) List<@Size(max = 100) String> orderedWords,
        Boolean showedAnswer,
        @Pattern(regexp = "ru|en") String translationLanguage,
        @Min(0) @Max(86400) Integer durationSeconds,
        @Pattern(regexp = "audio_order|audio_dictation|pinyin_dictation|translation_order") String exerciseType
) {
}
