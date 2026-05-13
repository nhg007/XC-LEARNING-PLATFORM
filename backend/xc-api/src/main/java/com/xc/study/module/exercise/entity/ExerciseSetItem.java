package com.xc.study.module.exercise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("exercise_set_items")
public class ExerciseSetItem extends BaseEntity {

    private Long exerciseSetId;
    private Long sentenceExerciseId;
    private Integer sortOrder;
    private String status;

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
