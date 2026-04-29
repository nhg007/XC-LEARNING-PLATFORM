package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class AdminOperationLogQueryDTO {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    @Size(max = 40)
    private String sortBy;

    @Pattern(regexp = "asc|desc")
    private String sortDirection;

    private Long adminUserId;

    @Size(max = 100)
    private String action;

    @Size(max = 100)
    private String targetType;

    private Long targetId;

    private OffsetDateTime createdFrom;

    private OffsetDateTime createdTo;

    @Size(max = 100)
    private String keyword;

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

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
