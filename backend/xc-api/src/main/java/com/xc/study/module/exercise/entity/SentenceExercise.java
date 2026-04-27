package com.xc.study.module.exercise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("sentence_exercises")
public class SentenceExercise extends BaseEntity {

    private Long exerciseSetId;
    private String exerciseType;
    private String hanziAnswer;
    private String pinyinPrompt;
    private String translationEn;
    private String translationRu;
    private Long audioZhAssetId;
    private String explanation;
    private Integer sortOrder;
    private String status;

    public Long getExerciseSetId() {
        return exerciseSetId;
    }

    public void setExerciseSetId(Long exerciseSetId) {
        this.exerciseSetId = exerciseSetId;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
    }

    public String getHanziAnswer() {
        return hanziAnswer;
    }

    public void setHanziAnswer(String hanziAnswer) {
        this.hanziAnswer = hanziAnswer;
    }

    public String getPinyinPrompt() {
        return pinyinPrompt;
    }

    public void setPinyinPrompt(String pinyinPrompt) {
        this.pinyinPrompt = pinyinPrompt;
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

    public Long getAudioZhAssetId() {
        return audioZhAssetId;
    }

    public void setAudioZhAssetId(Long audioZhAssetId) {
        this.audioZhAssetId = audioZhAssetId;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
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
