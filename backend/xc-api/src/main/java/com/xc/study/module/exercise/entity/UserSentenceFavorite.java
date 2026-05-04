package com.xc.study.module.exercise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("user_sentence_favorites")
public class UserSentenceFavorite extends BaseEntity {

    private Long userId;
    private Long sentenceExerciseId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSentenceExerciseId() {
        return sentenceExerciseId;
    }

    public void setSentenceExerciseId(Long sentenceExerciseId) {
        this.sentenceExerciseId = sentenceExerciseId;
    }
}
