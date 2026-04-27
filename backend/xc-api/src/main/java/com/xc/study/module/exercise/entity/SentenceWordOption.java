package com.xc.study.module.exercise.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xc.study.common.entity.BaseEntity;

@TableName("sentence_word_options")
public class SentenceWordOption extends BaseEntity {

    private Long exerciseId;
    private String wordText;
    private Integer correctOrder;

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getWordText() {
        return wordText;
    }

    public void setWordText(String wordText) {
        this.wordText = wordText;
    }

    public Integer getCorrectOrder() {
        return correctOrder;
    }

    public void setCorrectOrder(Integer correctOrder) {
        this.correctOrder = correctOrder;
    }
}
