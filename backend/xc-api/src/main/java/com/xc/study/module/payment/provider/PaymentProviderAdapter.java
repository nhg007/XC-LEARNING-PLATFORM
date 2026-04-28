package com.xc.study.module.payment.provider;

import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.payment.entity.PaymentOrder;
import java.util.Map;

public interface PaymentProviderAdapter {

    boolean supports(String provider);

    boolean mockPayment();

    PaymentCreateResult createPayment(String provider, PaymentOrder order, MembershipPlan plan);

    PaymentNotificationParseResult parseNotification(String provider, Map<String, Object> payload);
}
