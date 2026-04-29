package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class AdminPaymentOrderQueryDTO {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    @Size(max = 40)
    private String sortBy;

    @Pattern(regexp = "asc|desc")
    private String sortDirection;

    @Size(max = 128)
    private String keyword;

    @Pattern(regexp = "pending|paid|failed|refunded")
    private String status;

    @Pattern(regexp = "wechat_pay|alipay|offline")
    private String provider;

    @Pattern(regexp = "web|mobile|admin")
    private String clientType;

    @Pattern(regexp = "all|pending_timeout|callback_failed|amount_mismatch|provider_mismatch|membership_missing")
    private String exceptionType;

    @Min(1)
    @Max(1440)
    private Integer pendingTimeoutMinutes;

    private OffsetDateTime createdFrom;

    private OffsetDateTime createdTo;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getExceptionType() {
        return exceptionType;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public Integer getPendingTimeoutMinutes() {
        return pendingTimeoutMinutes;
    }

    public void setPendingTimeoutMinutes(Integer pendingTimeoutMinutes) {
        this.pendingTimeoutMinutes = pendingTimeoutMinutes;
    }

    public OffsetDateTime getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(OffsetDateTime createdFrom) {
        this.createdFrom = createdFrom;
    }

    public OffsetDateTime getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(OffsetDateTime createdTo) {
        this.createdTo = createdTo;
    }
}
