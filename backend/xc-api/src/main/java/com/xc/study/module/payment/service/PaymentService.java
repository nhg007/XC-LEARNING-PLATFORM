package com.xc.study.module.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.payment.dto.CreatePaymentOrderDTO;
import com.xc.study.module.payment.entity.PaymentNotification;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.payment.provider.PaymentCreateResult;
import com.xc.study.module.payment.provider.PaymentNotificationParseResult;
import com.xc.study.module.payment.provider.PaymentProviderAdapter;
import com.xc.study.module.payment.provider.PaymentProviderRegistry;
import com.xc.study.module.payment.vo.PaymentNotificationResultVO;
import com.xc.study.module.payment.vo.PaymentOrderVO;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PaymentService {

    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String NOTIFY_HANDLED = "handled";
    private static final String NOTIFY_IGNORED = "ignored";
    private static final String NOTIFY_FAILED = "failed";
    private static final String RESULT_PAID_HANDLED = "PAID_HANDLED";
    private static final String RESULT_DUPLICATE_PAID = "DUPLICATE_PAID";
    private static final String RESULT_NOT_PAID = "NOT_PAID";
    private static final String RESULT_INVALID_SIGNATURE = "INVALID_SIGNATURE";
    private static final String RESULT_ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    private static final String RESULT_PROVIDER_MISMATCH = "PROVIDER_MISMATCH";
    private static final String RESULT_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    private static final String RESULT_ORDER_STATE_INVALID = "ORDER_STATE_INVALID";

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentNotificationRecorder paymentNotificationRecorder;
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final PaymentProviderRegistry paymentProviderRegistry;

    public PaymentService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentNotificationRecorder paymentNotificationRecorder,
            MembershipPlanMapper membershipPlanMapper,
            UserMembershipMapper userMembershipMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            PaymentProviderRegistry paymentProviderRegistry
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentNotificationRecorder = paymentNotificationRecorder;
        this.membershipPlanMapper = membershipPlanMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.paymentProviderRegistry = paymentProviderRegistry;
    }

    @Transactional
    public PaymentOrderVO createOrder(Long userId, CreatePaymentOrderDTO request) {
        User user = userMapper.selectById(userId);
        if (user == null || !"active".equals(user.getStatus())) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "账号状态不可创建订单");
        }
        MembershipPlan plan = requireActivePlan(request.planId());
        PaymentProviderAdapter providerAdapter = paymentProviderRegistry.requireAdapter(request.provider());

        OffsetDateTime now = OffsetDateTime.now();
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setAmount(plan.getPrice());
        order.setCurrency(plan.getCurrency());
        order.setProvider(request.provider());
        order.setClientType(StringUtils.hasText(request.clientType()) ? request.clientType() : "web");
        PaymentCreateResult createResult = providerAdapter.createPayment(request.provider(), order, plan);
        order.setPaymentUrl(createResult.paymentUrl());
        order.setProviderTradeNo(createResult.providerTradeNo());
        order.setStatus("pending");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        paymentOrderMapper.insert(order);
        return toOrderVO(order, plan);
    }

    @Transactional
    public PaymentOrder createOfflinePaidOrder(
            Long userId,
            Long planId,
            BigDecimal amount,
            String currency,
            OffsetDateTime paidAt,
            String offlineTradeNo
    ) {
        User user = userMapper.selectById(userId);
        if (user == null || !"active".equals(user.getStatus())) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "账号状态不可开通会员");
        }
        MembershipPlan plan = requireActivePlan(planId);
        OffsetDateTime now = OffsetDateTime.now();
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setAmount(amount == null ? plan.getPrice() : amount);
        order.setCurrency(StringUtils.hasText(currency) ? currency.trim().toUpperCase(Locale.ROOT) : plan.getCurrency());
        order.setProvider("offline");
        order.setClientType("admin");
        order.setPaymentUrl(null);
        order.setStatus("pending");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        paymentOrderMapper.insert(order);

        OffsetDateTime confirmedPaidAt = paidAt == null ? now : paidAt;
        String tradeNo = StringUtils.hasText(offlineTradeNo) ? offlineTradeNo.trim() : "OFFLINE-" + order.getOrderNo();
        markOrderPaid(order, plan, tradeNo, confirmedPaidAt);
        return order;
    }

    public PaymentOrderVO getStudentOrder(Long userId, String orderNo) {
        PaymentOrder order = requireOrder(orderNo);
        if (!Objects.equals(order.getUserId(), userId)) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "无权查看该订单");
        }
        MembershipPlan plan = membershipPlanMapper.selectById(order.getPlanId());
        return toOrderVO(order, plan);
    }

    @Transactional
    public PaymentOrderVO simulatePaid(Long userId, String orderNo) {
        PaymentOrder order = requireOrder(orderNo);
        if (!paymentProviderRegistry.isMockProvider(order.getProvider())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前环境未启用模拟支付");
        }
        if (!Objects.equals(order.getUserId(), userId)) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        MembershipPlan plan = requirePlan(order.getPlanId());
        markOrderPaid(order, plan, "MOCK-" + order.getOrderNo(), OffsetDateTime.now());
        return toOrderVO(order, plan);
    }

    @Transactional
    public PaymentNotificationResultVO handleNotification(String provider, Map<String, Object> payload) {
        PaymentProviderAdapter providerAdapter = paymentProviderRegistry.requireAdapter(provider);
        PaymentNotificationParseResult parseResult = providerAdapter.parseNotification(provider, payload == null ? Map.of() : payload);
        PaymentOrder order = StringUtils.hasText(parseResult.orderNo()) ? findOrder(parseResult.orderNo()) : null;
        PaymentNotification notification = buildNotification(provider, parseResult.providerTradeNo(), payload, order == null ? null : order.getId());
        notification.setSignatureValid(parseResult.signatureValid());

        if (!parseResult.signatureValid()) {
            recordNotification(notification, false, NOTIFY_FAILED, RESULT_INVALID_SIGNATURE, "支付回调验签失败");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付回调验签失败");
        }

        if (order == null) {
            recordNotification(notification, false, NOTIFY_FAILED, RESULT_ORDER_NOT_FOUND, "订单不存在");
            throw BusinessException.notFound("订单不存在");
        }
        if (!provider.equals(order.getProvider())) {
            recordNotification(notification, false, NOTIFY_FAILED, RESULT_PROVIDER_MISMATCH, "支付渠道不匹配");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付渠道不匹配");
        }
        if (!parseResult.paid()) {
            recordNotification(notification, false, NOTIFY_IGNORED, RESULT_NOT_PAID, "支付渠道状态不是成功支付");
            return new PaymentNotificationResultVO(
                    order.getOrderNo(),
                    order.getStatus(),
                    false,
                    NOTIFY_IGNORED,
                    RESULT_NOT_PAID,
                    "支付渠道状态不是成功支付"
            );
        }

        MembershipPlan plan = requirePlan(order.getPlanId());
        if (!amountMatches(parseResult.amount(), order.getAmount())) {
            recordNotification(notification, false, NOTIFY_FAILED, RESULT_AMOUNT_MISMATCH, "支付金额不匹配");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额不匹配");
        }

        PaymentMarkResult markResult;
        try {
            markResult = markOrderPaid(
                    order,
                    plan,
                    StringUtils.hasText(parseResult.providerTradeNo()) ? parseResult.providerTradeNo() : provider.toUpperCase(Locale.ROOT) + "-" + order.getOrderNo(),
                    OffsetDateTime.now()
            );
        } catch (BusinessException ex) {
            recordNotification(notification, false, NOTIFY_FAILED, RESULT_ORDER_STATE_INVALID, ex.getMessage());
            throw ex;
        }

        String resultCode = markResult.newlyPaid() ? RESULT_PAID_HANDLED : RESULT_DUPLICATE_PAID;
        String message = markResult.newlyPaid() ? "支付成功，会员已开通" : "订单已支付，重复回调已忽略";
        recordNotification(notification, true, NOTIFY_HANDLED, resultCode, message);
        return new PaymentNotificationResultVO(order.getOrderNo(), order.getStatus(), true, NOTIFY_HANDLED, resultCode, message);
    }

    private PaymentMarkResult markOrderPaid(PaymentOrder order, MembershipPlan plan, String providerTradeNo, OffsetDateTime paidAt) {
        if ("paid".equals(order.getStatus())) {
            grantMembership(order, plan, order.getPaidAt() == null ? paidAt : order.getPaidAt());
            return new PaymentMarkResult(false);
        }
        if (!"pending".equals(order.getStatus())) {
            throw BusinessException.conflict("订单状态不可支付");
        }

        int updated = paymentOrderMapper.markPendingOrderPaid(order.getId(), providerTradeNo, paidAt);
        if (updated == 0) {
            PaymentOrder latest = paymentOrderMapper.selectById(order.getId());
            if (latest != null && "paid".equals(latest.getStatus())) {
                copyPaidState(order, latest);
                grantMembership(order, plan, latest.getPaidAt() == null ? paidAt : latest.getPaidAt());
                return new PaymentMarkResult(false);
            }
            throw BusinessException.conflict("订单状态不可支付");
        }

        applyPaidState(order, providerTradeNo, paidAt);
        grantMembership(order, plan, paidAt);
        return new PaymentMarkResult(true);
    }

    private void grantMembership(PaymentOrder order, MembershipPlan plan, OffsetDateTime now) {
        UserMembership existing = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getPaymentOrderId, order.getId())
                .last("limit 1"));
        if (existing != null) {
            return;
        }

        UserMembership current = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, order.getUserId())
                .eq(UserMembership::getStatus, "active")
                .gt(UserMembership::getEndsAt, now)
                .orderByDesc(UserMembership::getEndsAt)
                .last("limit 1"));
        OffsetDateTime startedAt = current != null && current.getEndsAt().isAfter(now) ? current.getEndsAt() : now;
        UserMembership membership = new UserMembership();
        membership.setUserId(order.getUserId());
        membership.setPlanId(plan.getId());
        membership.setPaymentOrderId(order.getId());
        membership.setStartedAt(startedAt);
        membership.setEndsAt(startedAt.plusDays(plan.getDurationDays()));
        membership.setSource("payment");
        membership.setStatus("active");
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        userMembershipMapper.insert(membership);
    }

    private void applyPaidState(PaymentOrder order, String providerTradeNo, OffsetDateTime paidAt) {
        order.setStatus("paid");
        order.setPaidAt(paidAt);
        order.setProviderTradeNo(providerTradeNo);
        order.setUpdatedAt(paidAt);
    }

    private void copyPaidState(PaymentOrder target, PaymentOrder source) {
        target.setStatus(source.getStatus());
        target.setPaidAt(source.getPaidAt());
        target.setProviderTradeNo(source.getProviderTradeNo());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private PaymentNotification buildNotification(String provider, String tradeNo, Map<String, Object> payload, Long orderId) {
        OffsetDateTime now = OffsetDateTime.now();
        PaymentNotification notification = new PaymentNotification();
        notification.setOrderId(orderId);
        notification.setProvider(provider);
        notification.setProviderTradeNo(tradeNo);
        notification.setNotifyPayload(toJson(payload));
        notification.setSignatureValid(false);
        notification.setHandled(false);
        notification.setReceivedAt(now);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        return notification;
    }

    private MembershipPlan requireActivePlan(Long planId) {
        MembershipPlan plan = requirePlan(planId);
        if (!"active".equals(plan.getStatus())) {
            throw BusinessException.notFound("会员套餐不存在");
        }
        return plan;
    }

    private MembershipPlan requirePlan(Long planId) {
        MembershipPlan plan = membershipPlanMapper.selectById(planId);
        if (plan == null) {
            throw BusinessException.notFound("会员套餐不存在");
        }
        return plan;
    }

    private PaymentOrder requireOrder(String orderNo) {
        PaymentOrder order = findOrder(orderNo);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        return order;
    }

    private PaymentOrder findOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, orderNo)
                .last("limit 1"));
    }

    private PaymentOrderVO toOrderVO(PaymentOrder order, MembershipPlan plan) {
        return new PaymentOrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getPlanId(),
                plan == null ? null : plan.getName(),
                order.getAmount(),
                order.getCurrency(),
                order.getProvider(),
                order.getClientType(),
                order.getPaymentUrl(),
                order.getProviderTradeNo(),
                order.getStatus(),
                order.getPaidAt(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                paymentProviderRegistry.isMockProvider(order.getProvider())
        );
    }

    private boolean amountMatches(BigDecimal amountValue, BigDecimal orderAmount) {
        if (amountValue == null) {
            return true;
        }
        return amountValue.compareTo(orderAmount) == 0;
    }

    private void recordNotification(
            PaymentNotification notification,
            boolean handled,
            String processStatus,
            String resultCode,
            String resultMessage
    ) {
        notification.setHandled(handled);
        notification.setProcessStatus(processStatus);
        notification.setResultCode(resultCode);
        notification.setResultMessage(resultMessage);
        paymentNotificationRecorder.record(notification);
    }

    private String nextOrderNo() {
        byte[] bytes = new byte[6];
        RANDOM.nextBytes(bytes);
        return "XC" + LocalDate.now().format(ORDER_DATE_FORMAT) + HexFormat.of().formatHex(bytes).toUpperCase(Locale.ROOT);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private record PaymentMarkResult(boolean newlyPaid) {
    }
}
