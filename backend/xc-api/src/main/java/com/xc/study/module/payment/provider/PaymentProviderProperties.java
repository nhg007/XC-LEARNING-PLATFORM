package com.xc.study.module.payment.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderProperties {

    private final boolean mockEnabled;
    private final ProviderConfig wechatPay;
    private final ProviderConfig alipay;

    public PaymentProviderProperties(
            @Value("${app.payment.mock-enabled:false}") boolean mockEnabled,
            @Value("${app.payment.wechat-pay.enabled:false}") boolean wechatPayEnabled,
            @Value("${app.payment.wechat-pay.payment-url-prefix:}") String wechatPayPaymentUrlPrefix,
            @Value("${app.payment.wechat-pay.notify-secret:}") String wechatPayNotifySecret,
            @Value("${app.payment.alipay.enabled:false}") boolean alipayEnabled,
            @Value("${app.payment.alipay.payment-url-prefix:}") String alipayPaymentUrlPrefix,
            @Value("${app.payment.alipay.notify-secret:}") String alipayNotifySecret
    ) {
        this.mockEnabled = mockEnabled;
        this.wechatPay = new ProviderConfig(wechatPayEnabled, wechatPayPaymentUrlPrefix, wechatPayNotifySecret);
        this.alipay = new ProviderConfig(alipayEnabled, alipayPaymentUrlPrefix, alipayNotifySecret);
    }

    public boolean mockEnabled() {
        return mockEnabled;
    }

    public ProviderConfig provider(String provider) {
        return switch (provider) {
            case "wechat_pay" -> wechatPay;
            case "alipay" -> alipay;
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
