package com.xc.study.module.payment.provider;

import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.payment.entity.PaymentOrder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SignedPayloadPaymentProviderAdapter implements PaymentProviderAdapter {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final PaymentProviderProperties properties;

    public SignedPayloadPaymentProviderAdapter(PaymentProviderProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean supports(String provider) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        return config.enabled();
    }

    @Override
    public boolean mockPayment() {
        return false;
    }

    @Override
    public PaymentCreateResult createPayment(String provider, PaymentOrder order, MembershipPlan plan) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        if (!StringUtils.hasText(config.paymentUrlPrefix())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付链接前缀未配置");
        }
        String separator = config.paymentUrlPrefix().contains("?") ? "&" : "?";
        return new PaymentCreateResult(config.paymentUrlPrefix() + separator + "orderNo=" + order.getOrderNo(), null);
    }

    @Override
    public PaymentNotificationParseResult parseNotification(String provider, Map<String, Object> payload) {
        String status = PaymentPayloads.status(payload);
        return new PaymentNotificationParseResult(
                verifySignature(provider, payload),
                PaymentPayloads.paid(status),
                PaymentPayloads.orderNo(payload),
                PaymentPayloads.tradeNo(payload),
                PaymentPayloads.amount(payload),
                status
        );
    }

    private boolean verifySignature(String provider, Map<String, Object> payload) {
        PaymentProviderProperties.ProviderConfig config = properties.provider(provider);
        String secret = config.notifySecret();
        String signature = PaymentPayloads.stringValue(payload.get("signature"));
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(signature)) {
            return false;
        }
        return hmac(canonicalPayload(payload), secret).equalsIgnoreCase(signature);
    }

    private String canonicalPayload(Map<String, Object> payload) {
        return payload.entrySet()
                .stream()
                .filter(entry -> !"signature".equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
    }

    private String hmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "支付回调验签失败");
        }
    }
}
