package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("user_vocab_progress")
public class UserVocabProgress extends BaseEntity {

    private Long userId;
    private Long vocabListId;
    private Integer currentIndex;
    private Long lastVocabItemId;
    private Integer reviewedCount;

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

    public Integer getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(Integer currentIndex) {
        this.currentIndex = currentIndex;
    }

    public Long getLastVocabItemId() {
        return lastVocabItemId;
    }

    public void setLastVocabItemId(Long lastVocabItemId) {
        this.lastVocabItemId = lastVocabItemId;
    }

    public Integer getReviewedCount() {
        return reviewedCount;
    }

    public void setReviewedCount(Integer reviewedCount) {
        this.reviewedCount = reviewedCount;
    }
}
