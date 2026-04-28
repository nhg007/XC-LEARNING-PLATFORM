package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUpsertDialogueLineVocabDTO(
        @NotNull Long dialogueLineId,
        Long vocabItemId,
        @NotBlank @Size(max = 100) String wordText,
        @Size(max = 200) String pinyin,
        @Size(max = 4000) String meaningEn,
        @Size(max = 4000) String meaningRu,
        @Size(max = 4000) String explanation
) {
}
