package com.xc.study.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("user_preferences")
public class UserPreference extends BaseEntity {

    private Long userId;
    private String uiLanguage;
    private String translationLanguage;
    private String vocabMeaningLanguage;
    private String matchingMeaningLanguage;
    private Boolean soundEnabled;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUiLanguage() {
        return uiLanguage;
    }

    public void setUiLanguage(String uiLanguage) {
        this.uiLanguage = uiLanguage;
    }

    public String getTranslationLanguage() {
        return translationLanguage;
    }

    public void setTranslationLanguage(String translationLanguage) {
        this.translationLanguage = translationLanguage;
    }

    public String getVocabMeaningLanguage() {
        return vocabMeaningLanguage;
    }

    public void setVocabMeaningLanguage(String vocabMeaningLanguage) {
        this.vocabMeaningLanguage = vocabMeaningLanguage;
    }

    public String getMatchingMeaningLanguage() {
        return matchingMeaningLanguage;
    }

    public void setMatchingMeaningLanguage(String matchingMeaningLanguage) {
        this.matchingMeaningLanguage = matchingMeaningLanguage;
    }

    public Boolean getSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(Boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }
}
