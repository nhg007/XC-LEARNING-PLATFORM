package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("vocab_list_items")
public class VocabListItem extends BaseEntity {

    private Long vocabListId;
    private Long vocabItemId;
    private Integer sortOrder;
    private String status;

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
