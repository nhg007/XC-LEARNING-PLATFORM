package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class AdminOrderExceptionSummaryQueryDTO {

    @Min(1)
    @Max(1440)
    private Integer pendingTimeoutMinutes;

    public Integer getPendingTimeoutMinutes() {
        return pendingTimeoutMinutes;
    }

    public void setPendingTimeoutMinutes(Integer pendingTimeoutMinutes) {
        this.pendingTimeoutMinutes = pendingTimeoutMinutes;
    }
}
