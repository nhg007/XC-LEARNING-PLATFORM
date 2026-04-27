package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("user_vocab_favorites")
public class UserVocabFavorite extends BaseEntity {

    private Long userId;
    private Long vocabItemId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getVocabItemId() {
        return vocabItemId;
    }

    public void setVocabItemId(Long vocabItemId) {
        this.vocabItemId = vocabItemId;
    }
}
