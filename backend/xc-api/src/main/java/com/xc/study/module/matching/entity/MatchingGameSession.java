package com.xc.study.module.matching.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.time.OffsetDateTime;

@TableName("matching_game_sessions")
public class MatchingGameSession extends BaseEntity {

    private Long userId;
    private String sourceType;
    private Long vocabListId;
    private String meaningLanguage;
    private String difficulty;
    private Integer totalPairs;
    private Integer matchedPairs;
    private Integer wrongCount;
    private Integer elapsedSeconds;
    private String status;
    private OffsetDateTime completedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getVocabListId() {
        return vocabListId;
    }

    public void setVocabListId(Long vocabListId) {
        this.vocabListId = vocabListId;
    }

    public String getMeaningLanguage() {
        return meaningLanguage;
    }

    public void setMeaningLanguage(String meaningLanguage) {
        this.meaningLanguage = meaningLanguage;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getTotalPairs() {
        return totalPairs;
    }

    public void setTotalPairs(Integer totalPairs) {
        this.totalPairs = totalPairs;
    }

    public Integer getMatchedPairs() {
        return matchedPairs;
    }

    public void setMatchedPairs(Integer matchedPairs) {
        this.matchedPairs = matchedPairs;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public Integer getElapsedSeconds() {
        return elapsedSeconds;
    }

    public void setElapsedSeconds(Integer elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
