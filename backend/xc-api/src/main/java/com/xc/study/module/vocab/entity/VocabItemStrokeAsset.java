package com.xc.study.module.vocab.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("vocab_item_stroke_assets")
public class VocabItemStrokeAsset extends BaseEntity {

    private Long vocabItemId;
    private Long mediaAssetId;
    private String title;
    private Integer sortOrder;
    private String status;

    public Long getVocabItemId() {
        return vocabItemId;
    }

    public void setVocabItemId(Long vocabItemId) {
        this.vocabItemId = vocabItemId;
    }

    public Long getMediaAssetId() {
        return mediaAssetId;
    }

    public void setMediaAssetId(Long mediaAssetId) {
        this.mediaAssetId = mediaAssetId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
