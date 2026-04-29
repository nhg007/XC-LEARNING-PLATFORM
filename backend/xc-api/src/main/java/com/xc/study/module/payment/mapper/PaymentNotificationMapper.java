package com.xc.study.module.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xc.study.module.admin.dto.AdminPaymentNotificationQueryDTO;
import com.xc.study.module.payment.entity.PaymentNotification;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentNotificationMapper extends BaseMapper<PaymentNotification> {

    @Select("""
            <script>
            select
              id,
              order_id,
              provider,
              provider_trade_no,
              notify_payload::text as notify_payload,
              signature_valid,
              handled,
              process_status,
              result_code,
              result_message,
              received_at,
              created_at,
              updated_at
            from payment_notifications
            <where>
              <if test="query.provider != null and query.provider != ''">
                and provider = #{query.provider}
              </if>
              <if test="query.processStatus != null and query.processStatus != ''">
                and process_status = #{query.processStatus}
              </if>
              <if test="query.resultCode != null and query.resultCode != ''">
                and result_code = #{query.resultCode}
              </if>
              <if test="query.signatureValid != null">
                and signature_valid = #{query.signatureValid}
              </if>
              <if test="query.orderId != null">
                and order_id = #{query.orderId}
              </if>
              <if test="query.receivedFrom != null">
                and received_at &gt;= #{query.receivedFrom}
              </if>
              <if test="query.receivedTo != null">
                and received_at &lt;= #{query.receivedTo}
              </if>
              <if test="keyword != null and keyword != ''">
                and (
                  provider_trade_no like concat('%', #{keyword}, '%')
                  or result_code like concat('%', #{keyword}, '%')
                  or result_message like concat('%', #{keyword}, '%')
                  <if test="numericOrderId != null">
                    or order_id = #{numericOrderId}
                  </if>
                  <if test="orderIds != null and orderIds.size > 0">
                    or order_id in
                    <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                      #{orderId}
                    </foreach>
                  </if>
                )
              </if>
            </where>
            order by received_at desc, id desc
            </script>
            """)
    Page<PaymentNotification> selectNotificationPage(
            Page<PaymentNotification> page,
            @Param("query") AdminPaymentNotificationQueryDTO query,
            @Param("keyword") String keyword,
            @Param("numericOrderId") Long numericOrderId,
            @Param("orderIds") List<Long> orderIds
    );

    @Select("""
            <script>
            select distinct on (order_id)
              id,
              order_id,
              provider,
              provider_trade_no,
              notify_payload::text as notify_payload,
              signature_valid,
              handled,
              process_status,
              result_code,
              result_message,
              received_at,
              created_at,
              updated_at
            from payment_notifications
            where order_id in
              <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
              </foreach>
            order by order_id, received_at desc, id desc
            </script>
            """)
    List<PaymentNotification> selectLatestByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Select("""
            <script>
            select
              id,
              order_id,
              provider,
              provider_trade_no,
              notify_payload::text as notify_payload,
              signature_valid,
              handled,
              process_status,
              result_code,
              result_message,
              received_at,
              created_at,
              updated_at
            from payment_notifications
            where process_status = 'failed'
              and order_id in
              <foreach collection="orderIds" item="orderId" open="(" separator="," close=")">
                #{orderId}
              </foreach>
            order by received_at desc, id desc
            </script>
            """)
    List<PaymentNotification> selectFailedResultsByOrderIds(@Param("orderIds") List<Long> orderIds);

    @Insert("""
            insert into payment_notifications (
              order_id, provider, provider_trade_no, notify_payload, signature_valid, handled,
              process_status, result_code, result_message,
              received_at, created_at, updated_at
            ) values (
              #{orderId}, #{provider}, #{providerTradeNo}, cast(#{notifyPayload} as jsonb),
              #{signatureValid}, #{handled}, #{processStatus}, #{resultCode}, #{resultMessage},
              #{receivedAt}, #{createdAt}, #{updatedAt}
            )
            """)
    int insertNotification(PaymentNotification notification);
}
