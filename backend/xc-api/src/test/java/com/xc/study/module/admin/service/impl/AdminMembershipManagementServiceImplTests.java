package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.admin.dto.AdminCreateOfflinePaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminOrderExceptionSummaryQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.vo.AdminOrderExceptionSummaryVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.payment.entity.PaymentNotification;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentNotificationMapper;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.payment.service.PaymentService;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.UserType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMembershipManagementServiceImplTests {

    @Test
    void orderExceptionSummaryReturnsKnownBuckets() {
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        AdminMembershipManagementServiceImpl service = service(paymentOrderMapper);

        when(paymentOrderMapper.countExceptionOrders(any())).thenReturn(7L);
        when(paymentOrderMapper.countPendingTimeoutOrders(any())).thenReturn(2L);
        when(paymentOrderMapper.countFailedNotificationOrders()).thenReturn(4L);
        when(paymentOrderMapper.countFailedNotificationOrdersByResultCode("AMOUNT_MISMATCH")).thenReturn(1L);
        when(paymentOrderMapper.countFailedNotificationOrdersByResultCode("PROVIDER_MISMATCH")).thenReturn(1L);
        when(paymentOrderMapper.countPaidOrdersMissingMembership()).thenReturn(3L);

        AdminOrderExceptionSummaryVO summary = service.orderExceptionSummary(new AdminOrderExceptionSummaryQueryDTO(), admin());

        assertEquals(7L, summary.allExceptions());
        assertEquals(2L, summary.pendingTimeout());
        assertEquals(4L, summary.callbackFailed());
        assertEquals(1L, summary.amountMismatch());
        assertEquals(1L, summary.providerMismatch());
        assertEquals(3L, summary.membershipMissing());
    }

    @Test
    void pageOrdersAddsExceptionTagsAndLatestCallback() {
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        UserMembershipMapper userMembershipMapper = mock(UserMembershipMapper.class);
        MembershipPlanMapper membershipPlanMapper = mock(MembershipPlanMapper.class);
        PaymentNotificationMapper paymentNotificationMapper = mock(PaymentNotificationMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AdminMembershipManagementServiceImpl service = new AdminMembershipManagementServiceImpl(
                membershipPlanMapper,
                userMembershipMapper,
                paymentOrderMapper,
                paymentNotificationMapper,
                mock(PaymentService.class),
                userMapper,
                mock(AdminOperationLogMapper.class),
                new ObjectMapper(),
                mock(MasterDataCache.class)
        );

        PaymentOrder order = order();
        Page<PaymentOrder> page = Page.of(1, 20);
        page.setRecords(List.of(order));
        page.setTotal(1);
        when(paymentOrderMapper.selectPage(any(), any())).thenReturn(page);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user()));
        when(membershipPlanMapper.selectBatchIds(any())).thenReturn(List.of(plan()));
        PaymentNotification notification = failedAmountNotification();
        when(paymentNotificationMapper.selectLatestByOrderIds(List.of(10L))).thenReturn(List.of(notification));
        when(paymentNotificationMapper.selectFailedResultsByOrderIds(List.of(10L))).thenReturn(List.of(notification));
        when(userMembershipMapper.selectList(any())).thenReturn(List.of());

        AdminPaymentOrderQueryDTO query = new AdminPaymentOrderQueryDTO();
        query.setPendingTimeoutMinutes(30);
        PageResult<AdminPaymentOrderVO> result = service.pageOrders(query, admin());

        AdminPaymentOrderVO item = result.records().get(0);
        assertEquals(List.of("pending_timeout", "callback_failed", "amount_mismatch"), item.exceptionTypes());
        assertEquals("failed", item.latestNotificationProcessStatus());
        assertEquals("AMOUNT_MISMATCH", item.latestNotificationResultCode());
    }

    @Test
    void pageOrderOperationLogsUsesOrderPermissionAndTargetFilter() {
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        AdminOperationLogMapper operationLogMapper = mock(AdminOperationLogMapper.class);
        AdminMembershipManagementServiceImpl service = new AdminMembershipManagementServiceImpl(
                mock(MembershipPlanMapper.class),
                mock(UserMembershipMapper.class),
                paymentOrderMapper,
                mock(PaymentNotificationMapper.class),
                mock(PaymentService.class),
                mock(UserMapper.class),
                operationLogMapper,
                new ObjectMapper(),
                mock(MasterDataCache.class)
        );

        when(paymentOrderMapper.selectById(10L)).thenReturn(order());
        AdminOperationLog log = operationLog();
        Page<AdminOperationLog> page = Page.of(1, 20);
        page.setRecords(List.of(log));
        page.setTotal(1);
        when(operationLogMapper.selectTargetLogPage(any(), eq("payment_order"), eq(10L))).thenReturn(page);

        PageResult<AdminOperationLogVO> result = service.pageOrderOperationLogs(10L, 1, 20, admin());

        AdminOperationLogVO item = result.records().get(0);
        assertEquals("payment.order.mark_failed", item.action());
        assertEquals("{\"reason\":\"用户放弃支付\"}", item.detail());
    }

    @Test
    void createOfflinePaymentOrderCreatesPaidOrderAndAuditLog() {
        MembershipPlanMapper membershipPlanMapper = mock(MembershipPlanMapper.class);
        UserMembershipMapper userMembershipMapper = mock(UserMembershipMapper.class);
        PaymentOrderMapper paymentOrderMapper = mock(PaymentOrderMapper.class);
        PaymentNotificationMapper paymentNotificationMapper = mock(PaymentNotificationMapper.class);
        PaymentService paymentService = mock(PaymentService.class);
        UserMapper userMapper = mock(UserMapper.class);
        AdminOperationLogMapper operationLogMapper = mock(AdminOperationLogMapper.class);
        AdminMembershipManagementServiceImpl service = new AdminMembershipManagementServiceImpl(
                membershipPlanMapper,
                userMembershipMapper,
                paymentOrderMapper,
                paymentNotificationMapper,
                paymentService,
                userMapper,
                operationLogMapper,
                new ObjectMapper(),
                mock(MasterDataCache.class)
        );

        OffsetDateTime paidAt = OffsetDateTime.now();
        PaymentOrder paidOrder = order();
        paidOrder.setProvider("offline");
        paidOrder.setClientType("admin");
        paidOrder.setProviderTradeNo("CASH-1");
        paidOrder.setStatus("paid");
        paidOrder.setPaidAt(paidAt);
        AdminCreateOfflinePaymentOrderDTO request = new AdminCreateOfflinePaymentOrderDTO(
                "student@example.com",
                20L,
                new BigDecimal("29.00"),
                "CNY",
                paidAt,
                "CASH-1",
                "线下现金收款"
        );

        when(userMapper.selectList(any())).thenReturn(List.of(user()));
        when(paymentService.createOfflinePaidOrder(100L, 20L, new BigDecimal("29.00"), "CNY", paidAt, "CASH-1"))
                .thenReturn(paidOrder);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user()));
        when(membershipPlanMapper.selectBatchIds(any())).thenReturn(List.of(plan()));
        when(paymentNotificationMapper.selectLatestByOrderIds(List.of(10L))).thenReturn(List.of());
        when(paymentNotificationMapper.selectFailedResultsByOrderIds(List.of(10L))).thenReturn(List.of());
        when(userMembershipMapper.selectList(any())).thenReturn(List.of());

        AdminPaymentOrderVO result = service.createOfflinePaymentOrder(request, admin(), "127.0.0.1");

        assertEquals("offline", result.provider());
        assertEquals("paid", result.status());
        ArgumentCaptor<AdminOperationLog> logCaptor = ArgumentCaptor.forClass(AdminOperationLog.class);
        verify(operationLogMapper).insertLog(logCaptor.capture());
        assertEquals("payment.order.offline_paid", logCaptor.getValue().getAction());
        assertEquals("payment_order", logCaptor.getValue().getTargetType());
        assertEquals(10L, logCaptor.getValue().getTargetId());
    }

    private AdminMembershipManagementServiceImpl service(PaymentOrderMapper paymentOrderMapper) {
        return new AdminMembershipManagementServiceImpl(
                mock(MembershipPlanMapper.class),
                mock(UserMembershipMapper.class),
                paymentOrderMapper,
                mock(PaymentNotificationMapper.class),
                mock(PaymentService.class),
                mock(UserMapper.class),
                mock(AdminOperationLogMapper.class),
                new ObjectMapper(),
                mock(MasterDataCache.class)
        );
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin", UserType.ADMIN, Set.of("super_admin"), Set.of("admin:*"));
    }

    private PaymentOrder order() {
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
        order.setCreatedAt(OffsetDateTime.now().minusHours(2));
        order.setUpdatedAt(OffsetDateTime.now().minusHours(2));
        return order;
    }

    private User user() {
        User user = new User();
        user.setId(100L);
        user.setEmail("student@example.com");
        user.setNickname("Student");
        return user;
    }

    private MembershipPlan plan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(20L);
        plan.setName("月度会员");
        return plan;
    }

    private PaymentNotification failedAmountNotification() {
        PaymentNotification notification = new PaymentNotification();
        notification.setId(30L);
        notification.setOrderId(10L);
        notification.setProvider("wechat_pay");
        notification.setProcessStatus("failed");
        notification.setResultCode("AMOUNT_MISMATCH");
        notification.setResultMessage("支付金额不匹配");
        notification.setReceivedAt(OffsetDateTime.now().minusMinutes(10));
        notification.setCreatedAt(notification.getReceivedAt());
        notification.setUpdatedAt(notification.getReceivedAt());
        return notification;
    }

    private AdminOperationLog operationLog() {
        AdminOperationLog log = new AdminOperationLog();
        log.setId(50L);
        log.setAdminUserId(1L);
        log.setAction("payment.order.mark_failed");
        log.setTargetType("payment_order");
        log.setTargetId(10L);
        log.setDetail("{\"reason\":\"用户放弃支付\"}");
        log.setIpAddress("127.0.0.1");
        log.setCreatedAt(OffsetDateTime.now());
        log.setUpdatedAt(log.getCreatedAt());
        return log;
    }
}
