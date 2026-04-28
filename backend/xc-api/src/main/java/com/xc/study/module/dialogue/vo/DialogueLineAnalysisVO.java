package com.xc.study.module.dialogue.vo;

import java.util.List;

public record DialogueLineAnalysisVO(
        Long lineId,
        String hanziText,
        String pinyinText,
        String translationEn,
        String translationRu,
        List<DialogueLineVocabVO> vocabItems
) {
}
