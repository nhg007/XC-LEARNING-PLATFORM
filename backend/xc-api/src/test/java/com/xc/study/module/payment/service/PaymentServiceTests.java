package com.xc.study.module.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.payment.entity.PaymentNotification;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.payment.provider.PaymentNotificationParseResult;
import com.xc.study.module.payment.provider.PaymentProviderAdapter;
import com.xc.study.module.payment.provider.PaymentProviderRegistry;
import com.xc.study.module.payment.vo.PaymentOrderVO;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.common.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Test
    void simulatePaidOnlyGrantsMembershipOnceForSameOrder() {
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        PaymentNotificationRecorder paymentNotificationRecorder = mock(PaymentNotificationRecorder.class);
        MembershipPlanMapper membershipPlanMapper = mock(MembershipPlanMapper.class);
        UserMembershipMapper userMembershipMapper = mock(UserMembershipMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PaymentProviderRegistry paymentProviderRegistry = mock(PaymentProviderRegistry.class);
        PaymentService paymentService = new PaymentService(
                paymentOrderMapper,
                paymentNotificationRecorder,
                membershipPlanMapper,
                userMembershipMapper,
                userMapper,
                new ObjectMapper(),
                paymentProviderRegistry
        );

        PaymentOrder order = pendingOrder();
        MembershipPlan plan = activePlan();
        when(paymentOrderMapper.selectOne(any())).thenReturn(order);
        when(paymentProviderRegistry.isMockProvider("wechat_pay")).thenReturn(true);
        when(membershipPlanMapper.selectById(20L)).thenReturn(plan);
        when(paymentOrderMapper.markPendingOrderPaid(eq(10L), eq("MOCK-XC202604290001"), any(OffsetDateTime.class)))
                .thenReturn(1);
        UserMembership existingMembership = new UserMembership();
        existingMembership.setPaymentOrderId(10L);
        when(userMembershipMapper.selectOne(any())).thenReturn(null).thenReturn(null).thenReturn(existingMembership);

        PaymentOrderVO first = paymentService.simulatePaid(100L, "XC202604290001");
        PaymentOrderVO second = paymentService.simulatePaid(100L, "XC202604290001");

        assertEquals("paid", first.status());
        assertEquals("paid", second.status());
        verify(paymentOrderMapper, times(1))
                .markPendingOrderPaid(eq(10L), eq("MOCK-XC202604290001"), any(OffsetDateTime.class));

        ArgumentCaptor<UserMembership> membershipCaptor = ArgumentCaptor.forClass(UserMembership.class);
        verify(userMembershipMapper, times(1)).insert(membershipCaptor.capture());
        UserMembership membership = membershipCaptor.getValue();
        assertEquals(100L, membership.getUserId());
        assertEquals(20L, membership.getPlanId());
        assertEquals(10L, membership.getPaymentOrderId());
        assertEquals("payment", membership.getSource());
        assertEquals("active", membership.getStatus());
    }

    @Test
    void handleNotificationRecordsAmountMismatchFailure() {
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        PaymentNotificationRecorder paymentNotificationRecorder = mock(PaymentNotificationRecorder.class);
        MembershipPlanMapper membershipPlanMapper = mock(MembershipPlanMapper.class);
        UserMembershipMapper userMembershipMapper = mock(UserMembershipMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        PaymentProviderRegistry paymentProviderRegistry = mock(PaymentProviderRegistry.class);
        PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
        PaymentService paymentService = new PaymentService(
                paymentOrderMapper,
                paymentNotificationRecorder,
                membershipPlanMapper,
                userMembershipMapper,
                userMapper,
                new ObjectMapper(),
                paymentProviderRegistry
        );

        when(paymentProviderRegistry.requireAdapter("wechat_pay")).thenReturn(adapter);
        when(adapter.parseNotification(eq("wechat_pay"), any())).thenReturn(new PaymentNotificationParseResult(
                true,
                true,
                "XC202604290001",
                "WX-TRADE-1",
                new BigDecimal("28.00"),
                "SUCCESS"
        ));
        when(paymentOrderMapper.selectOne(any())).thenReturn(pendingOrder());
        when(membershipPlanMapper.selectById(20L)).thenReturn(activePlan());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> paymentService.handleNotification("wechat_pay", java.util.Map.of("orderNo", "XC202604290001"))
        );

        assertEquals("支付金额不匹配", ex.getMessage());
        ArgumentCaptor<PaymentNotification> notificationCaptor = ArgumentCaptor.forClass(PaymentNotification.class);
        verify(paymentNotificationRecorder).record(notificationCaptor.capture());
        PaymentNotification notification = notificationCaptor.getValue();
        assertFalse(notification.getHandled());
        assertEquals("failed", notification.getProcessStatus());
        assertEquals("AMOUNT_MISMATCH", notification.getResultCode());
        assertEquals("支付金额不匹配", notification.getResultMessage());
    }

    private PaymentOrder pendingOrder() {
        OffsetDateTime now = OffsetDateTime.now();
        PaymentOrder order = new PaymentOrder();
        order.setId(10L);
        order.setOrderNo("XC202604290001");
        order.setUserId(100L);
        order.setPlanId(20L);
        order.setAmount(new BigDecimal("29.00"));
        order.setCurrency("CNY");
        order.setProvider("wechat_pay");
        order.setClientType("web");
        order.setPaymentUrl("mock-payment://wechat_pay/XC202604290001");
        order.setStatus("pending");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        return order;
    }

    private MembershipPlan activePlan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(20L);
        plan.setName("月度会员");
        plan.setDurationDays(30);
        plan.setDurationUnit("month");
        plan.setDurationValue(1);
        plan.setPrice(new BigDecimal("29.00"));
        plan.setCurrency("CNY");
        plan.setStatus("active");
        return plan;
    }
}
