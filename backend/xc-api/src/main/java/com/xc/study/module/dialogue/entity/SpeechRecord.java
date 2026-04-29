package com.xc.study.module.dialogue.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.math.BigDecimal;

@TableName("speech_records")
public class SpeechRecord extends BaseEntity {

    private Long userId;
    private Long dialogueLineId;
    private Long audioAssetId;
    private String recognizedText;
    private String compareResult;
    private BigDecimal score;
    private Long asrJobId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDialogueLineId() {
        return dialogueLineId;
    }

    public void setDialogueLineId(Long dialogueLineId) {
        this.dialogueLineId = dialogueLineId;
    }

    public Long getAudioAssetId() {
        return audioAssetId;
    }

    public void setAudioAssetId(Long audioAssetId) {
        this.audioAssetId = audioAssetId;
    }

    public String getRecognizedText() {
        return recognizedText;
    }

    public void setRecognizedText(String recognizedText) {
        this.recognizedText = recognizedText;
    }

    public String getCompareResult() {
        return compareResult;
    }

    public void setCompareResult(String compareResult) {
        this.compareResult = compareResult;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Long getAsrJobId() {
        return asrJobId;
    }

    public void setAsrJobId(Long asrJobId) {
        this.asrJobId = asrJobId;
    }
}
