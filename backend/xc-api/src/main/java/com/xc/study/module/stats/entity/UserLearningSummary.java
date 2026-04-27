package com.xc.study.module.stats.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("user_learning_summary")
public class UserLearningSummary extends BaseEntity {

    private Long userId;
    private Integer totalStudySeconds;
    private Integer totalExerciseCount;
    private Integer totalCorrectCount;
    private Integer totalVocabReviewCount;
    private Integer currentStreakDays;
    private Integer longestStreakDays;
    private BigDecimal overallAccuracyRate;
    private LocalDate lastStudyDate;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTotalStudySeconds() {
        return totalStudySeconds;
    }

    public void setTotalStudySeconds(Integer totalStudySeconds) {
        this.totalStudySeconds = totalStudySeconds;
    }

    public Integer getTotalExerciseCount() {
        return totalExerciseCount;
    }

    public void setTotalExerciseCount(Integer totalExerciseCount) {
        this.totalExerciseCount = totalExerciseCount;
    }

    public Integer getTotalCorrectCount() {
        return totalCorrectCount;
    }

    public void setTotalCorrectCount(Integer totalCorrectCount) {
        this.totalCorrectCount = totalCorrectCount;
    }

    public Integer getTotalVocabReviewCount() {
        return totalVocabReviewCount;
    }

    public void setTotalVocabReviewCount(Integer totalVocabReviewCount) {
        this.totalVocabReviewCount = totalVocabReviewCount;
    }

    public Integer getCurrentStreakDays() {
        return currentStreakDays;
    }

    public void setCurrentStreakDays(Integer currentStreakDays) {
        this.currentStreakDays = currentStreakDays;
    }

    public Integer getLongestStreakDays() {
        return longestStreakDays;
    }

    public void setLongestStreakDays(Integer longestStreakDays) {
        this.longestStreakDays = longestStreakDays;
    }

    public BigDecimal getOverallAccuracyRate() {
        return overallAccuracyRate;
    }

    public void setOverallAccuracyRate(BigDecimal overallAccuracyRate) {
        this.overallAccuracyRate = overallAccuracyRate;
    }

    public LocalDate getLastStudyDate() {
        return lastStudyDate;
    }

    public void setLastStudyDate(LocalDate lastStudyDate) {
        this.lastStudyDate = lastStudyDate;
    }
}
