package com.xc.study.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xc.study.module.payment.entity.PaymentOrder;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("""
            select coalesce(sum(amount), 0)
            from payment_orders
            where status = 'paid'
              and paid_at >= #{from}
            """)
    BigDecimal sumPaidAmountFrom(@Param("from") OffsetDateTime from);

    @Select("""
            select count(*)
            from payment_orders
            where status = 'pending'
              and created_at <= #{pendingCutoff}
            """)
    long countPendingTimeoutOrders(@Param("pendingCutoff") OffsetDateTime pendingCutoff);

    @Select("""
            select count(*)
            from payment_orders order_item
            where (
                order_item.status = 'pending'
                and order_item.created_at <= #{pendingCutoff}
              )
              or exists (
                select 1
                from payment_notifications notification
                where notification.order_id = order_item.id
                  and notification.process_status = 'failed'
              )
              or (
                order_item.status = 'paid'
                and not exists (
                  select 1
                  from user_memberships membership
                  where membership.payment_order_id = order_item.id
                )
              )
            """)
    long countExceptionOrders(@Param("pendingCutoff") OffsetDateTime pendingCutoff);

    @Select("""
            select count(distinct order_id)
            from payment_notifications
            where process_status = 'failed'
            """)
    long countFailedNotificationOrders();

    @Select("""
            select count(distinct order_id)
            from payment_notifications
            where process_status = 'failed'
              and result_code = #{resultCode}
            """)
    long countFailedNotificationOrdersByResultCode(@Param("resultCode") String resultCode);

    @Select("""
            select count(*)
            from payment_orders order_item
            where order_item.status = 'paid'
              and not exists (
                select 1
                from user_memberships membership
                where membership.payment_order_id = order_item.id
              )
            """)
    long countPaidOrdersMissingMembership();

    @Update("""
            update payment_orders
            set status = 'paid',
                provider_trade_no = #{providerTradeNo},
                paid_at = #{paidAt},
                updated_at = #{paidAt}
            where id = #{orderId}
              and status = 'pending'
            """)
    int markPendingOrderPaid(
            @Param("orderId") Long orderId,
            @Param("providerTradeNo") String providerTradeNo,
            @Param("paidAt") OffsetDateTime paidAt
    );

    @Update("""
            update payment_orders
            set status = 'failed',
                updated_at = #{updatedAt}
            where id = #{orderId}
              and status = 'pending'
            """)
    int markPendingOrderFailed(
            @Param("orderId") Long orderId,
            @Param("updatedAt") OffsetDateTime updatedAt
    );
}
