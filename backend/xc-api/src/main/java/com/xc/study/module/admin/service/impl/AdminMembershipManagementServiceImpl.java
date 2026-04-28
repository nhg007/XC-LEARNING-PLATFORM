package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminMembershipPlanQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateMembershipPlanStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertMembershipPlanDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminMembershipManagementService;
import com.xc.study.module.admin.vo.AdminMembershipPlanVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.payment.entity.PaymentOrder;
import com.xc.study.module.payment.mapper.PaymentOrderMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminMembershipManagementServiceImpl implements AdminMembershipManagementService {

    private final MembershipPlanMapper membershipPlanMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final UserMapper userMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminMembershipManagementServiceImpl(
            MembershipPlanMapper membershipPlanMapper,
            PaymentOrderMapper paymentOrderMapper,
            UserMapper userMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.membershipPlanMapper = membershipPlanMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.userMapper = userMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AdminMembershipPlanVO> pagePlans(AdminMembershipPlanQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:memberships:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<MembershipPlan> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(MembershipPlan::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(MembershipPlan::getName, query.getKeyword().trim());
        }
        wrapper.orderByDesc(MembershipPlan::getCreatedAt);
        Page<MembershipPlan> result = membershipPlanMapper.selectPage(Page.of(page, pageSize), wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toPlanVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    @Transactional
    public AdminMembershipPlanVO createPlan(AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:memberships:update");
        OffsetDateTime now = OffsetDateTime.now();
        MembershipPlan plan = new MembershipPlan();
        fillPlan(plan, request);
        plan.setStatus(StringUtils.hasText(request.status()) ? request.status() : "active");
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        membershipPlanMapper.insert(plan);
        writeOperationLog(admin.id(), "membership.plan.create", "membership_plan", plan.getId(), Map.of(
                "name", plan.getName(),
                "durationDays", plan.getDurationDays(),
                "price", plan.getPrice(),
                "currency", plan.getCurrency(),
                "status", plan.getStatus()
        ), ipAddress);
        return toPlanVO(plan);
    }

    @Override
    @Transactional
    public AdminMembershipPlanVO updatePlan(Long planId, AdminUpsertMembershipPlanDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:memberships:update");
        MembershipPlan plan = requirePlan(planId);
        Map<String, Object> before = planSnapshot(plan);
        fillPlan(plan, request);
        if (StringUtils.hasText(request.status())) {
            plan.setStatus(request.status());
        }
        plan.setUpdatedAt(OffsetDateTime.now());
        membershipPlanMapper.updateById(plan);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("before", before);
        detail.put("after", planSnapshot(plan));
        writeOperationLog(admin.id(), "membership.plan.update", "membership_plan", planId, detail, ipAddress);
        return toPlanVO(plan);
    }

    @Override
    @Transactional
    public AdminMembershipPlanVO updatePlanStatus(
            Long planId,
            AdminUpdateMembershipPlanStatusDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:memberships:update");
        MembershipPlan plan = requirePlan(planId);
        String beforeStatus = plan.getStatus();
        plan.setStatus(request.status());
        plan.setUpdatedAt(OffsetDateTime.now());
        membershipPlanMapper.updateById(plan);
        writeOperationLog(admin.id(), "membership.plan.status.update", "membership_plan", planId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", request.reason() == null ? "" : request.reason()
        ), ipAddress);
        return toPlanVO(plan);
    }

    @Override
    public PageResult<AdminPaymentOrderVO> pageOrders(AdminPaymentOrderQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:orders:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<PaymentOrder> wrapper = buildOrderWrapper(query);
        wrapper.orderByDesc(PaymentOrder::getCreatedAt);
        Page<PaymentOrder> result = paymentOrderMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminPaymentOrderVO> records = toOrderVOs(result.getRecords());
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminPaymentOrderVO getOrderDetail(Long orderId, CurrentUser admin) {
        requirePermission(admin, "admin:orders:read");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        return toOrderVOs(List.of(order)).get(0);
    }

    private LambdaQueryWrapper<PaymentOrder> buildOrderWrapper(AdminPaymentOrderQueryDTO query) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(PaymentOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getProvider())) {
            wrapper.eq(PaymentOrder::getProvider, query.getProvider());
        }
        if (StringUtils.hasText(query.getClientType())) {
            wrapper.eq(PaymentOrder::getClientType, query.getClientType());
        }
        if (query.getCreatedFrom() != null) {
            wrapper.ge(PaymentOrder::getCreatedAt, query.getCreatedFrom());
        }
        if (query.getCreatedTo() != null) {
            wrapper.le(PaymentOrder::getCreatedAt, query.getCreatedTo());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getEmail, keyword)
                            .or()
                            .like(User::getNickname, keyword))
                    .stream()
                    .map(User::getId)
                    .toList();
            wrapper.and(item -> {
                item.like(PaymentOrder::getOrderNo, keyword)
                        .or()
                        .like(PaymentOrder::getProviderTradeNo, keyword);
                if (!userIds.isEmpty()) {
                    item.or().in(PaymentOrder::getUserId, userIds);
                }
            });
        }
        return wrapper;
    }

    private List<AdminPaymentOrderVO> toOrderVOs(List<PaymentOrder> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> users = loadUsers(orders);
        Map<Long, MembershipPlan> plans = loadPlans(orders);
        return orders.stream()
                .map(order -> {
                    User user = users.get(order.getUserId());
                    MembershipPlan plan = plans.get(order.getPlanId());
                    return new AdminPaymentOrderVO(
                            order.getId(),
                            order.getOrderNo(),
                            order.getUserId(),
                            user == null ? null : user.getEmail(),
                            user == null ? null : user.getNickname(),
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
                            order.getUpdatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, User> loadUsers(List<PaymentOrder> orders) {
        List<Long> userIds = orders.stream().map(PaymentOrder::getUserId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, MembershipPlan> loadPlans(List<PaymentOrder> orders) {
        List<Long> planIds = orders.stream().map(PaymentOrder::getPlanId).filter(Objects::nonNull).distinct().toList();
        if (planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return membershipPlanMapper.selectBatchIds(planIds)
                .stream()
                .collect(Collectors.toMap(MembershipPlan::getId, Function.identity()));
    }

    private void fillPlan(MembershipPlan plan, AdminUpsertMembershipPlanDTO request) {
        plan.setName(request.name().trim());
        plan.setDurationUnit(request.durationUnit());
        plan.setDurationValue(request.durationValue());
        plan.setDurationDays(resolveDurationDays(request.durationUnit(), request.durationValue()));
        plan.setPrice(request.price());
        plan.setCurrency(request.currency().trim().toUpperCase());
    }

    private int resolveDurationDays(String unit, int value) {
        if ("month".equals(unit)) {
            return value * 30;
        }
        return value;
    }

    private MembershipPlan requirePlan(Long planId) {
        MembershipPlan plan = membershipPlanMapper.selectById(planId);
        if (plan == null) {
            throw BusinessException.notFound("会员套餐不存在");
        }
        return plan;
    }

    private AdminMembershipPlanVO toPlanVO(MembershipPlan plan) {
        return new AdminMembershipPlanVO(
                plan.getId(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getDurationUnit(),
                plan.getDurationValue(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getStatus(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private Map<String, Object> planSnapshot(MembershipPlan plan) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", plan.getName());
        snapshot.put("durationDays", plan.getDurationDays());
        snapshot.put("durationUnit", plan.getDurationUnit());
        snapshot.put("durationValue", plan.getDurationValue());
        snapshot.put("price", plan.getPrice());
        snapshot.put("currency", plan.getCurrency());
        snapshot.put("status", plan.getStatus());
        return snapshot;
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new HashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
