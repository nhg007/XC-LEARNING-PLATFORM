package com.xc.study.module.dialogue.vo;

public record DialogueLineCheckResultVO(
        Long eventId,
        Long lineId,
        boolean correct,
        String answerText,
        String standardAnswer,
        Integer firstMismatchIndex,
        String message
) {
}
