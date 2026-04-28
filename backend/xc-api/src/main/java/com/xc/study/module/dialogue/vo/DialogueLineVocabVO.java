package com.xc.study.module.dialogue.vo;

public record DialogueLineVocabVO(
        Long id,
        Long vocabItemId,
        String wordText,
        String pinyin,
        String meaningEn,
        String meaningRu,
        String explanation
) {
}
