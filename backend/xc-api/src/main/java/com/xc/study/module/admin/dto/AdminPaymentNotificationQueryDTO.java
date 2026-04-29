package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public class AdminPaymentNotificationQueryDTO {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    @Size(max = 128)
    private String keyword;

    @Pattern(regexp = "wechat_pay|alipay")
    private String provider;

    @Pattern(regexp = "handled|ignored|failed")
    private String processStatus;

    @Size(max = 80)
    private String resultCode;

    private Boolean signatureValid;

    private Long orderId;

    private OffsetDateTime receivedFrom;

    private OffsetDateTime receivedTo;

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

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProcessStatus() {
        return processStatus;
    }

    public void setProcessStatus(String processStatus) {
        this.processStatus = processStatus;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public Boolean getSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(Boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OffsetDateTime getReceivedFrom() {
        return receivedFrom;
    }

    public void setReceivedFrom(OffsetDateTime receivedFrom) {
        this.receivedFrom = receivedFrom;
    }

    public OffsetDateTime getReceivedTo() {
        return receivedTo;
    }

    public void setReceivedTo(OffsetDateTime receivedTo) {
        this.receivedTo = receivedTo;
    }
}
