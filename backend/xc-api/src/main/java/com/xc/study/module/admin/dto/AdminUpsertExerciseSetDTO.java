package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpsertExerciseSetDTO(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Pattern(regexp = "audio_order|audio_dictation|pinyin_dictation|translation_order") String exerciseType,
        @Size(max = 20) String level,
        @Pattern(regexp = "active|inactive") String status
) {
}
