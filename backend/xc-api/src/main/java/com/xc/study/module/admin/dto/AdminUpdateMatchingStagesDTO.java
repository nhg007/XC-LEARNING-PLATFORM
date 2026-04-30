package com.xc.study.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class AdminUpdateMatchingStagesDTO {

    @NotEmpty
    @Size(max = 20)
    @Valid
    private List<StageItem> stages;

    public List<StageItem> getStages() {
        return stages;
    }

    public void setStages(List<StageItem> stages) {
        this.stages = stages;
    }

    public static class StageItem {

        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,30}$")
        private String code;

        @NotNull
        @Size(max = 3)
        private Map<String, String> labels;

        @NotNull
        @Min(2)
        @Max(30)
        private Integer pairCount;

        @NotNull
        private Boolean enabled;

        @NotNull
        private Integer sortOrder;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public Integer getPairCount() {
            return pairCount;
        }

        public void setPairCount(Integer pairCount) {
            this.pairCount = pairCount;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}
