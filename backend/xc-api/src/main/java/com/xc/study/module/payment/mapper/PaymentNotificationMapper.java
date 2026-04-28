package com.xc.study.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.payment.entity.PaymentNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentNotificationMapper extends BaseMapper<PaymentNotification> {

    @Insert("""
            insert into payment_notifications (
              order_id, provider, provider_trade_no, notify_payload, signature_valid, handled,
              received_at, created_at, updated_at
            ) values (
              #{orderId}, #{provider}, #{providerTradeNo}, cast(#{notifyPayload} as jsonb),
              #{signatureValid}, #{handled}, #{receivedAt}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertNotification(PaymentNotification notification);
}
