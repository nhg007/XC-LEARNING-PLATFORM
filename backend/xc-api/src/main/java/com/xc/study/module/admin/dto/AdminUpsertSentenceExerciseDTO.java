package com.xc.study.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUpsertSentenceExerciseDTO(
        Long exerciseSetId,
        @NotBlank @Pattern(regexp = "audio_order|audio_dictation|pinyin_dictation|translation_order") String exerciseType,
        @NotBlank @Size(max = 1000) String hanziAnswer,
        @Size(max = 1000) String pinyinPrompt,
        @Size(max = 2000) String translationEn,
        @Size(max = 2000) String translationRu,
        Long audioZhAssetId,
        @Size(max = 4000) String explanation,
        @NotNull @Min(0) @Max(999999) Integer sortOrder,
        @Pattern(regexp = "active|inactive") String status,
        @Valid @Size(max = 100) List<AdminSentenceWordOptionDTO> wordOptions,
        @Size(max = 100) List<Long> exerciseSetIds
) {
}
