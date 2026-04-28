package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Size;

public class AdminUpdateSystemConfigDTO {

    @Size(max = 2000)
    private String configValue;

    @Size(max = 500)
    private String description;

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
