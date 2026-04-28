package com.xc.study.module.payment.provider;

import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {

    private final List<PaymentProviderAdapter> adapters;

    public PaymentProviderRegistry(List<PaymentProviderAdapter> adapters) {
        this.adapters = adapters.stream()
                .sorted(Comparator.comparing(PaymentProviderAdapter::mockPayment).reversed())
                .toList();
    }

    public PaymentProviderAdapter requireAdapter(String provider) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(provider))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "支付渠道尚未配置"));
    }

    public boolean isMockProvider(String provider) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(provider))
                .findFirst()
                .map(PaymentProviderAdapter::mockPayment)
                .orElse(false);
    }
}
