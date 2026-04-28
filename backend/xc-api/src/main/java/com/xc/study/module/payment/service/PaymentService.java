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
import com.xc.study.module.payment.mapper.PaymentNotificationMapper;
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

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentNotificationMapper paymentNotificationMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final PaymentProviderRegistry paymentProviderRegistry;

    public PaymentService(
            PaymentOrderMapper paymentOrderMapper,
            PaymentNotificationMapper paymentNotificationMapper,
            MembershipPlanMapper membershipPlanMapper,
            UserMembershipMapper userMembershipMapper,
            UserMapper userMapper,
            ObjectMapper objectMapper,
            PaymentProviderRegistry paymentProviderRegistry
    ) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentNotificationMapper = paymentNotificationMapper;
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
            notification.setSignatureValid(false);
            notification.setHandled(false);
            paymentNotificationMapper.insertNotification(notification);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付回调验签失败");
        }

        if (order == null) {
            notification.setHandled(false);
            paymentNotificationMapper.insertNotification(notification);
            throw BusinessException.notFound("订单不存在");
        }
        if (!provider.equals(order.getProvider())) {
            notification.setHandled(false);
            paymentNotificationMapper.insertNotification(notification);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付渠道不匹配");
        }
        if (!parseResult.paid()) {
            notification.setHandled(false);
            paymentNotificationMapper.insertNotification(notification);
            return new PaymentNotificationResultVO(order.getOrderNo(), order.getStatus(), false);
        }

        MembershipPlan plan = requirePlan(order.getPlanId());
        validateAmount(parseResult.amount(), order.getAmount());
        markOrderPaid(
                order,
                plan,
                StringUtils.hasText(parseResult.providerTradeNo()) ? parseResult.providerTradeNo() : provider.toUpperCase(Locale.ROOT) + "-" + order.getOrderNo(),
                OffsetDateTime.now()
        );
        notification.setHandled(true);
        paymentNotificationMapper.insertNotification(notification);
        return new PaymentNotificationResultVO(order.getOrderNo(), order.getStatus(), true);
    }

    private void markOrderPaid(PaymentOrder order, MembershipPlan plan, String providerTradeNo, OffsetDateTime paidAt) {
        if ("paid".equals(order.getStatus())) {
            return;
        }
        if (!"pending".equals(order.getStatus())) {
            throw BusinessException.conflict("订单状态不可支付");
        }
        order.setStatus("paid");
        order.setPaidAt(paidAt);
        order.setProviderTradeNo(providerTradeNo);
        order.setUpdatedAt(paidAt);
        paymentOrderMapper.updateById(order);
        grantMembership(order, plan, paidAt);
    }

    private void grantMembership(PaymentOrder order, MembershipPlan plan, OffsetDateTime now) {
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
        membership.setStartedAt(startedAt);
        membership.setEndsAt(startedAt.plusDays(plan.getDurationDays()));
        membership.setSource("payment");
        membership.setStatus("active");
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        userMembershipMapper.insert(membership);
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

    private void validateAmount(BigDecimal amountValue, BigDecimal orderAmount) {
        if (amountValue == null) {
            return;
        }
        if (amountValue.compareTo(orderAmount) != 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "支付金额不匹配");
        }
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
}
