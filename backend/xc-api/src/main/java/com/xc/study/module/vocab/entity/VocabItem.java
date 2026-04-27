package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("vocab_items")
public class VocabItem extends BaseEntity {

    private Long vocabListId;
    private String hanzi;
    private String pinyin;
    private String meaningEn;
    private String meaningRu;
    private String exampleSentence;
    private Long audioAssetId;
    private Integer sortOrder;
    private String status;

    public Long getVocabListId() {
        return vocabListId;
    }

    public void setVocabListId(Long vocabListId) {
        this.vocabListId = vocabListId;
    }

    public String getHanzi() {
        return hanzi;
    }

    public void setHanzi(String hanzi) {
        this.hanzi = hanzi;
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

    public String getExampleSentence() {
        return exampleSentence;
    }

    public void setExampleSentence(String exampleSentence) {
        this.exampleSentence = exampleSentence;
    }

    public Long getAudioAssetId() {
        return audioAssetId;
    }

    public void setAudioAssetId(Long audioAssetId) {
        this.audioAssetId = audioAssetId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
