package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;
import java.time.OffsetDateTime;

@TableName("user_vocab_item_progress")
public class UserVocabItemProgress extends BaseEntity {

    private Long userId;
    private Long vocabListId;
    private Long vocabItemId;
    private String status;
    private Integer reviewCount;
    private OffsetDateTime learnedAt;
    private OffsetDateTime lastReviewedAt;
    private OffsetDateTime nextReviewAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVocabListId() {
        return vocabListId;
    }

    public void setVocabListId(Long vocabListId) {
        this.vocabListId = vocabListId;
    }

    public Long getVocabItemId() {
        return vocabItemId;
    }

    public void setVocabItemId(Long vocabItemId) {
        this.vocabItemId = vocabItemId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public OffsetDateTime getLearnedAt() {
        return learnedAt;
    }

    public void setLearnedAt(OffsetDateTime learnedAt) {
        this.learnedAt = learnedAt;
    }

    public OffsetDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(OffsetDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public OffsetDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(OffsetDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }
}
