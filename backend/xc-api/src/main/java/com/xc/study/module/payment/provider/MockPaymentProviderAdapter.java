package com.xc.study.module.payment.provider;

import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.payment.entity.PaymentOrder;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentProviderAdapter implements PaymentProviderAdapter {

    private static final Set<String> PROVIDERS = Set.of("wechat_pay", "alipay");

    private final PaymentProviderProperties properties;

    public MockPaymentProviderAdapter(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String provider) {
        return properties.mockEnabled() && PROVIDERS.contains(provider);
    }

    @Override
    public boolean mockPayment() {
        return true;
    }

    @Override
    public PaymentCreateResult createPayment(String provider, PaymentOrder order, MembershipPlan plan) {
        return new PaymentCreateResult("mock-payment://" + provider + "/" + order.getOrderNo(), null);
    }

    @Override
    public PaymentNotificationParseResult parseNotification(String provider, Map<String, Object> payload) {
        String status = PaymentPayloads.status(payload);
        return new PaymentNotificationParseResult(
                true,
                PaymentPayloads.paid(status),
                PaymentPayloads.orderNo(payload),
                PaymentPayloads.tradeNo(payload),
                PaymentPayloads.amount(payload),
                status
        );
    }
}
