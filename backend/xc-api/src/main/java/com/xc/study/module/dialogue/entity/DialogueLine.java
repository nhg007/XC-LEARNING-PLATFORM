package com.xc.study.module.dialogue.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("dialogue_lines")
public class DialogueLine extends BaseEntity {

    private Long materialId;
    private Integer lineNo;
    private String hanziText;
    private String pinyinText;
    private String translationEn;
    private String translationRu;
    private Long audioAssetId;
    private Integer startMs;
    private Integer endMs;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getHanziText() {
        return hanziText;
    }

    public void setHanziText(String hanziText) {
        this.hanziText = hanziText;
    }

    public String getPinyinText() {
        return pinyinText;
    }

    public void setPinyinText(String pinyinText) {
        this.pinyinText = pinyinText;
    }

    public String getTranslationEn() {
        return translationEn;
    }

    public void setTranslationEn(String translationEn) {
        this.translationEn = translationEn;
    }

    public String getTranslationRu() {
        return translationRu;
    }

    public void setTranslationRu(String translationRu) {
        this.translationRu = translationRu;
    }

    public Long getAudioAssetId() {
        return audioAssetId;
    }

    public void setAudioAssetId(Long audioAssetId) {
        this.audioAssetId = audioAssetId;
    }

    public Integer getStartMs() {
        return startMs;
    }

    public void setStartMs(Integer startMs) {
        this.startMs = startMs;
    }

    public Integer getEndMs() {
        return endMs;
    }

    public void setEndMs(Integer endMs) {
        this.endMs = endMs;
    }
}
