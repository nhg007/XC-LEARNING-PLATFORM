package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminUpsertVocabItemDTO(
        Long vocabListId,
        @NotBlank @Size(max = 100) String hanzi,
        @Size(max = 200) String pinyin,
        @Size(max = 4000) String meaningEn,
        @Size(max = 4000) String meaningRu,
        @Size(max = 4000) String exampleSentence,
        Long audioAssetId,
        @NotNull @Min(0) @Max(999999) Integer sortOrder,
        @Pattern(regexp = "active|inactive") String status,
        @Size(max = 100) List<Long> vocabListIds
) {
}
