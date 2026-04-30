package com.xc.study.module.payment.provider;

import com.xc.study.module.admin.service.RuntimeConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderProperties {

    private final boolean envMockEnabled;
    private final ProviderConfig envWechatPay;
    private final ProviderConfig envAlipay;
    private final RuntimeConfigService runtimeConfigService;

    public PaymentProviderProperties(
            @Value("${app.payment.mock-enabled:false}") boolean mockEnabled,
            @Value("${app.payment.wechat-pay.enabled:false}") boolean wechatPayEnabled,
            @Value("${app.payment.wechat-pay.payment-url-prefix:}") String wechatPayPaymentUrlPrefix,
            @Value("${app.payment.wechat-pay.notify-secret:}") String wechatPayNotifySecret,
            @Value("${app.payment.alipay.enabled:false}") boolean alipayEnabled,
            @Value("${app.payment.alipay.payment-url-prefix:}") String alipayPaymentUrlPrefix,
            @Value("${app.payment.alipay.notify-secret:}") String alipayNotifySecret,
            RuntimeConfigService runtimeConfigService
    ) {
        this.envMockEnabled = mockEnabled;
        this.envWechatPay = new ProviderConfig(wechatPayEnabled, wechatPayPaymentUrlPrefix, wechatPayNotifySecret);
        this.envAlipay = new ProviderConfig(alipayEnabled, alipayPaymentUrlPrefix, alipayNotifySecret);
        this.runtimeConfigService = runtimeConfigService;
    }

    public boolean mockEnabled() {
        return runtimeConfigService.getBoolean(RuntimeConfigService.PAYMENT_MOCK_ENABLED, envMockEnabled);
    }

    public ProviderConfig provider(String provider) {
        return switch (provider) {
            case "wechat_pay" -> new ProviderConfig(
                    runtimeConfigService.getBoolean(RuntimeConfigService.PAYMENT_WECHAT_ENABLED, envWechatPay.enabled()),
                    runtimeConfigService.getString(RuntimeConfigService.PAYMENT_WECHAT_URL_PREFIX, envWechatPay.paymentUrlPrefix()),
                    runtimeConfigService.getString(RuntimeConfigService.PAYMENT_WECHAT_NOTIFY_SECRET, envWechatPay.notifySecret())
            );
            case "alipay" -> new ProviderConfig(
                    runtimeConfigService.getBoolean(RuntimeConfigService.PAYMENT_ALIPAY_ENABLED, envAlipay.enabled()),
                    runtimeConfigService.getString(RuntimeConfigService.PAYMENT_ALIPAY_URL_PREFIX, envAlipay.paymentUrlPrefix()),
                    runtimeConfigService.getString(RuntimeConfigService.PAYMENT_ALIPAY_NOTIFY_SECRET, envAlipay.notifySecret())
            );
            default -> new ProviderConfig(false, "", "");
        };
    }

    public record ProviderConfig(
            boolean enabled,
            String paymentUrlPrefix,
            String notifySecret
    ) {
    }
}
