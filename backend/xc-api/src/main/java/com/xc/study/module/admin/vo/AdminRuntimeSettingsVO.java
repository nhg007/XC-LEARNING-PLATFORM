package com.xc.study.module.admin.vo;

public record AdminRuntimeSettingsVO(
        MembershipSettings membership,
        PaymentSettings payment,
        AsrSettings asr,
        UploadSettings upload
) {

    public record MembershipSettings(Integer trialDays) {
    }

    public record PaymentSettings(
            Boolean mockEnabled,
            Boolean wechatPayEnabled,
            String wechatPayPaymentUrlPrefix,
            Boolean wechatPayNotifySecretConfigured,
            Boolean alipayEnabled,
            String alipayPaymentUrlPrefix,
            Boolean alipayNotifySecretConfigured
    ) {
    }

    public record AsrSettings(
            String provider,
            Boolean workerEnabled,
            String engineName,
            String serviceUrl,
            String servicePath,
            Long timeoutMs,
            Integer batchSize,
            Long initialDelayMs,
            Long pollDelayMs,
            String mockRecognizedText
    ) {
    }

    public record UploadSettings(
            String maxFileSize,
            String maxRequestSize,
            String audioExtensions,
            String imageExtensions,
            String videoExtensions
    ) {
    }
}
