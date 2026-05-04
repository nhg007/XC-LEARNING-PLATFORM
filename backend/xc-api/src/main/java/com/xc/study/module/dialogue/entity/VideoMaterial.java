package com.xc.study.module.dialogue.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("video_materials")
public class VideoMaterial extends BaseEntity {

    private String title;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long parentId;
    private String materialType;
    private String description;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long coverAssetId;
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCoverAssetId() {
        return coverAssetId;
    }

    public void setCoverAssetId(Long coverAssetId) {
        this.coverAssetId = coverAssetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
