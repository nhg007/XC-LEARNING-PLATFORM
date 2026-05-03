package com.xc.study.module.exercise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.time.OffsetDateTime;

@TableName("user_sentence_progress")
public class UserSentenceProgress extends BaseEntity {

    private Long userId;
    private Long exerciseSetId;
    private Long sentenceExerciseId;
    private String status;
    private Integer attemptCount;
    private Integer correctCount;
    private OffsetDateTime learnedAt;
    private OffsetDateTime lastPracticedAt;
    private OffsetDateTime lastCorrectAt;
    private OffsetDateTime nextReviewAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getExerciseSetId() {
        return exerciseSetId;
    }

    public void setExerciseSetId(Long exerciseSetId) {
        this.exerciseSetId = exerciseSetId;
    }

    public Long getSentenceExerciseId() {
        return sentenceExerciseId;
    }

    public void setSentenceExerciseId(Long sentenceExerciseId) {
        this.sentenceExerciseId = sentenceExerciseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public OffsetDateTime getLearnedAt() {
        return learnedAt;
    }

    public void setLearnedAt(OffsetDateTime learnedAt) {
        this.learnedAt = learnedAt;
    }

    public OffsetDateTime getLastPracticedAt() {
        return lastPracticedAt;
    }

    public void setLastPracticedAt(OffsetDateTime lastPracticedAt) {
        this.lastPracticedAt = lastPracticedAt;
    }

    public OffsetDateTime getLastCorrectAt() {
        return lastCorrectAt;
    }

    public void setLastCorrectAt(OffsetDateTime lastCorrectAt) {
        this.lastCorrectAt = lastCorrectAt;
    }

    public OffsetDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(OffsetDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }
}
