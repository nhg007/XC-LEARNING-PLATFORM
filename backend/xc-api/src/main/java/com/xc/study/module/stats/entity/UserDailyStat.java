package com.xc.study.module.stats.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("user_daily_stats")
public class UserDailyStat extends BaseEntity {

    private Long userId;
    private LocalDate statDate;
    private Integer studySeconds;
    private Integer exerciseCount;
    private Integer correctCount;
    private Integer vocabReviewCount;
    private Integer dialogueCount;
    private Integer matchingGameCount;
    private BigDecimal accuracyRate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getStudySeconds() {
        return studySeconds;
    }

    public void setStudySeconds(Integer studySeconds) {
        this.studySeconds = studySeconds;
    }

    public Integer getExerciseCount() {
        return exerciseCount;
    }

    public void setExerciseCount(Integer exerciseCount) {
        this.exerciseCount = exerciseCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getVocabReviewCount() {
        return vocabReviewCount;
    }

    public void setVocabReviewCount(Integer vocabReviewCount) {
        this.vocabReviewCount = vocabReviewCount;
    }

    public Integer getDialogueCount() {
        return dialogueCount;
    }

    public void setDialogueCount(Integer dialogueCount) {
        this.dialogueCount = dialogueCount;
    }

    public Integer getMatchingGameCount() {
        return matchingGameCount;
    }

    public void setMatchingGameCount(Integer matchingGameCount) {
        this.matchingGameCount = matchingGameCount;
    }

    public BigDecimal getAccuracyRate() {
        return accuracyRate;
    }

    public void setAccuracyRate(BigDecimal accuracyRate) {
        this.accuracyRate = accuracyRate;
    }
}
