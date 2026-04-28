package com.xc.study.module.dialogue.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("dialogue_line_vocab")
public class DialogueLineVocab extends BaseEntity {

    private Long dialogueLineId;
    private Long vocabItemId;
    private String wordText;
    private String pinyin;
    private String meaningEn;
    private String meaningRu;
    private String explanation;

    public Long getDialogueLineId() {
        return dialogueLineId;
    }

    public void setDialogueLineId(Long dialogueLineId) {
        this.dialogueLineId = dialogueLineId;
    }

    public Long getVocabItemId() {
        return vocabItemId;
    }

    public void setVocabItemId(Long vocabItemId) {
        this.vocabItemId = vocabItemId;
    }

    public String getWordText() {
        return wordText;
    }

    public void setWordText(String wordText) {
        this.wordText = wordText;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getMeaningEn() {
        return meaningEn;
    }

    public void setMeaningEn(String meaningEn) {
        this.meaningEn = meaningEn;
    }

    public String getMeaningRu() {
        return meaningRu;
    }

    public void setMeaningRu(String meaningRu) {
        this.meaningRu = meaningRu;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
