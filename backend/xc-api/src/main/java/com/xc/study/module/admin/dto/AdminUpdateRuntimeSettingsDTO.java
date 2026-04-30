package com.xc.study.module.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdminUpdateRuntimeSettingsDTO {

    @Valid
    private MembershipSettings membership;

    @Valid
    private PaymentSettings payment;

    @Valid
    private AsrSettings asr;

    @Valid
    private UploadSettings upload;

    public MembershipSettings getMembership() {
        return membership;
    }

    public void setMembership(MembershipSettings membership) {
        this.membership = membership;
    }

    public PaymentSettings getPayment() {
        return payment;
    }

    public void setPayment(PaymentSettings payment) {
        this.payment = payment;
    }

    public AsrSettings getAsr() {
        return asr;
    }

    public void setAsr(AsrSettings asr) {
        this.asr = asr;
    }

    public UploadSettings getUpload() {
        return upload;
    }

    public void setUpload(UploadSettings upload) {
        this.upload = upload;
    }

    public static class MembershipSettings {

        @Min(0)
        @Max(365)
        private Integer trialDays;

        public Integer getTrialDays() {
            return trialDays;
        }

        public void setTrialDays(Integer trialDays) {
            this.trialDays = trialDays;
        }
    }

    public static class PaymentSettings {

        private Boolean mockEnabled;
        private Boolean wechatPayEnabled;

        @Size(max = 500)
        private String wechatPayPaymentUrlPrefix;

        @Size(max = 200)
        private String wechatPayNotifySecret;

        private Boolean clearWechatPayNotifySecret;
        private Boolean alipayEnabled;

        @Size(max = 500)
        private String alipayPaymentUrlPrefix;

        @Size(max = 200)
        private String alipayNotifySecret;

        private Boolean clearAlipayNotifySecret;

        public Boolean getMockEnabled() {
            return mockEnabled;
        }

        public void setMockEnabled(Boolean mockEnabled) {
            this.mockEnabled = mockEnabled;
        }

        public Boolean getWechatPayEnabled() {
            return wechatPayEnabled;
        }

        public void setWechatPayEnabled(Boolean wechatPayEnabled) {
            this.wechatPayEnabled = wechatPayEnabled;
        }

        public String getWechatPayPaymentUrlPrefix() {
            return wechatPayPaymentUrlPrefix;
        }

        public void setWechatPayPaymentUrlPrefix(String wechatPayPaymentUrlPrefix) {
            this.wechatPayPaymentUrlPrefix = wechatPayPaymentUrlPrefix;
        }

        public String getWechatPayNotifySecret() {
            return wechatPayNotifySecret;
        }

        public void setWechatPayNotifySecret(String wechatPayNotifySecret) {
            this.wechatPayNotifySecret = wechatPayNotifySecret;
        }

        public Boolean getClearWechatPayNotifySecret() {
            return clearWechatPayNotifySecret;
        }

        public void setClearWechatPayNotifySecret(Boolean clearWechatPayNotifySecret) {
            this.clearWechatPayNotifySecret = clearWechatPayNotifySecret;
        }

        public Boolean getAlipayEnabled() {
            return alipayEnabled;
        }

        public void setAlipayEnabled(Boolean alipayEnabled) {
            this.alipayEnabled = alipayEnabled;
        }

        public String getAlipayPaymentUrlPrefix() {
            return alipayPaymentUrlPrefix;
        }

        public void setAlipayPaymentUrlPrefix(String alipayPaymentUrlPrefix) {
            this.alipayPaymentUrlPrefix = alipayPaymentUrlPrefix;
        }

        public String getAlipayNotifySecret() {
            return alipayNotifySecret;
        }

        public void setAlipayNotifySecret(String alipayNotifySecret) {
            this.alipayNotifySecret = alipayNotifySecret;
        }

        public Boolean getClearAlipayNotifySecret() {
            return clearAlipayNotifySecret;
        }

        public void setClearAlipayNotifySecret(Boolean clearAlipayNotifySecret) {
            this.clearAlipayNotifySecret = clearAlipayNotifySecret;
        }
    }

    public static class AsrSettings {

        @Pattern(regexp = "mock|http")
        private String provider;
        private Boolean workerEnabled;

        @Size(max = 100)
        private String engineName;

        @Size(max = 500)
        private String serviceUrl;

        @Size(max = 100)
        private String servicePath;

        @Min(1000)
        @Max(120000)
        private Long timeoutMs;

        @Min(1)
        @Max(100)
        private Integer batchSize;

        @Min(1000)
        @Max(3600000)
        private Long initialDelayMs;

        @Min(1000)
        @Max(3600000)
        private Long pollDelayMs;

        @Size(max = 2000)
        private String mockRecognizedText;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public Boolean getWorkerEnabled() {
            return workerEnabled;
        }

        public void setWorkerEnabled(Boolean workerEnabled) {
            this.workerEnabled = workerEnabled;
        }

        public String getEngineName() {
            return engineName;
        }

        public void setEngineName(String engineName) {
            this.engineName = engineName;
        }

        public String getServiceUrl() {
            return serviceUrl;
        }

        public void setServiceUrl(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public String getServicePath() {
            return servicePath;
        }

        public void setServicePath(String servicePath) {
            this.servicePath = servicePath;
        }

        public Long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Integer getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(Integer batchSize) {
            this.batchSize = batchSize;
        }

        public Long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(Long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public Long getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(Long pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public String getMockRecognizedText() {
            return mockRecognizedText;
        }

        public void setMockRecognizedText(String mockRecognizedText) {
            this.mockRecognizedText = mockRecognizedText;
        }
    }

    public static class UploadSettings {

        @Pattern(regexp = "^\\d+(KB|MB|GB)$")
        private String maxFileSize;

        @Pattern(regexp = "^\\d+(KB|MB|GB)$")
        private String maxRequestSize;

        @Size(max = 500)
        private String audioExtensions;

        @Size(max = 500)
        private String imageExtensions;

        @Size(max = 500)
        private String videoExtensions;

        public String getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public String getMaxRequestSize() {
            return maxRequestSize;
        }

        public void setMaxRequestSize(String maxRequestSize) {
            this.maxRequestSize = maxRequestSize;
        }

        public String getAudioExtensions() {
            return audioExtensions;
        }

        public void setAudioExtensions(String audioExtensions) {
            this.audioExtensions = audioExtensions;
        }

        public String getImageExtensions() {
            return imageExtensions;
        }

        public void setImageExtensions(String imageExtensions) {
            this.imageExtensions = imageExtensions;
        }

        public String getVideoExtensions() {
            return videoExtensions;
        }

        public void setVideoExtensions(String videoExtensions) {
            this.videoExtensions = videoExtensions;
        }
    }
}
