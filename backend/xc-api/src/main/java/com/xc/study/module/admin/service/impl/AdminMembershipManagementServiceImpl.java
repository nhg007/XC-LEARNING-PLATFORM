package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.module.admin.dto.AdminCreateOfflinePaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminFailPaymentOrderDTO;
import com.xc.study.module.admin.dto.AdminMembershipPlanQueryDTO;
import com.xc.study.module.admin.dto.AdminOrderExceptionSummaryQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentNotificationQueryDTO;
import com.xc.study.module.admin.dto.AdminPaymentOrderQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateMembershipPlanStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertMembershipPlanDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminMembershipManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminMembershipPlanVO;
import com.xc.study.module.admin.vo.AdminOrderExceptionSummaryVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPaymentNotificationVO;
import com.xc.study.module.admin.vo.AdminPaymentOrderVO;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
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
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminMembershipManagementServiceImpl implements AdminMembershipManagementService {

    private static final int DEFAULT_PENDING_TIMEOUT_MINUTES = 30;
    private static final String EXCEPTION_ALL = "all";
    private static final String EXCEPTION_PENDING_TIMEOUT = "pending_timeout";
    private static final String EXCEPTION_CALLBACK_FAILED = "callback_failed";
    private static final String EXCEPTION_AMOUNT_MISMATCH = "amount_mismatch";
    private static final String EXCEPTION_PROVIDER_MISMATCH = "provider_mismatch";
    private static final String EXCEPTION_MEMBERSHIP_MISSING = "membership_missing";
    private static final String RESULT_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    private static final String RESULT_PROVIDER_MISMATCH = "PROVIDER_MISMATCH";

    private final MembershipPlanMapper membershipPlanMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentNotificationMapper paymentNotificationMapper;
    private final PaymentService paymentService;
    private final UserMapper userMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final MasterDataCache masterDataCache;

    public AdminMembershipManagementServiceImpl(
            MembershipPlanMapper membershipPlanMapper,
            UserMembershipMapper userMembershipMapper,
            PaymentOrderMapper paymentOrderMapper,
            PaymentNotificationMapper paymentNotificationMapper,
            PaymentService paymentService,
            UserMapper userMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            MasterDataCache masterDataCache
    ) {
        this.membershipPlanMapper = membershipPlanMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentNotificationMapper = paymentNotificationMapper;
        this.paymentService = paymentService;
        this.userMapper = userMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.masterDataCache = masterDataCache;
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
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", MembershipPlan::getId,
                "name", MembershipPlan::getName,
                "durationDays", MembershipPlan::getDurationDays,
                "price", MembershipPlan::getPrice,
                "status", MembershipPlan::getStatus,
                "createdAt", MembershipPlan::getCreatedAt,
                "updatedAt", MembershipPlan::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(MembershipPlan::getCreatedAt);
        }
        wrapper.orderByDesc(MembershipPlan::getId);
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
        evictMembershipPlanCache();
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
        evictMembershipPlanCache();
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
        evictMembershipPlanCache();
        return toPlanVO(plan);
    }

    @Override
    public PageResult<AdminPaymentOrderVO> pageOrders(AdminPaymentOrderQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:orders:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        OffsetDateTime pendingCutoff = pendingTimeoutCutoff(query.getPendingTimeoutMinutes());
        LambdaQueryWrapper<PaymentOrder> wrapper = buildOrderWrapper(query, pendingCutoff);
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", PaymentOrder::getId,
                "orderNo", PaymentOrder::getOrderNo,
                "userId", PaymentOrder::getUserId,
                "planId", PaymentOrder::getPlanId,
                "amount", PaymentOrder::getAmount,
                "provider", PaymentOrder::getProvider,
                "clientType", PaymentOrder::getClientType,
                "status", PaymentOrder::getStatus,
                "paidAt", PaymentOrder::getPaidAt,
                "createdAt", PaymentOrder::getCreatedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(PaymentOrder::getCreatedAt);
        }
        wrapper.orderByDesc(PaymentOrder::getId);
        Page<PaymentOrder> result = paymentOrderMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminPaymentOrderVO> records = toOrderVOs(result.getRecords(), pendingCutoff);
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminPaymentOrderVO createOfflinePaymentOrder(
            AdminCreateOfflinePaymentOrderDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:orders:update");
        User user = resolveOfflinePaymentUser(request.userKeyword());
        PaymentOrder order = paymentService.createOfflinePaidOrder(
                user.getId(),
                request.planId(),
                request.amount(),
                request.currency(),
                request.paidAt(),
                request.offlineTradeNo()
        );
        writeOperationLog(admin.id(), "payment.order.offline_paid", "payment_order", order.getId(), Map.of(
                "orderNo", order.getOrderNo(),
                "userId", user.getId(),
                "userEmail", user.getEmail(),
                "planId", request.planId(),
                "amount", order.getAmount(),
                "currency", order.getCurrency(),
                "paidAt", order.getPaidAt(),
                "offlineTradeNo", order.getProviderTradeNo(),
                "remark", request.remark().trim()
        ), ipAddress);
        return toOrderVOs(List.of(order), pendingTimeoutCutoff(null)).get(0);
    }

    @Override
    public AdminOrderExceptionSummaryVO orderExceptionSummary(AdminOrderExceptionSummaryQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:orders:read");
        OffsetDateTime pendingCutoff = pendingTimeoutCutoff(query.getPendingTimeoutMinutes());
        return new AdminOrderExceptionSummaryVO(
                paymentOrderMapper.countExceptionOrders(pendingCutoff),
                paymentOrderMapper.countPendingTimeoutOrders(pendingCutoff),
                paymentOrderMapper.countFailedNotificationOrders(),
                paymentOrderMapper.countFailedNotificationOrdersByResultCode(RESULT_AMOUNT_MISMATCH),
                paymentOrderMapper.countFailedNotificationOrdersByResultCode(RESULT_PROVIDER_MISMATCH),
                paymentOrderMapper.countPaidOrdersMissingMembership()
        );
    }

    @Override
    public AdminPaymentOrderVO getOrderDetail(Long orderId, CurrentUser admin) {
        requirePermission(admin, "admin:orders:read");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        return toOrderVOs(List.of(order), pendingTimeoutCutoff(null)).get(0);
    }

    @Override
    public PageResult<AdminOperationLogVO> pageOrderOperationLogs(
            Long orderId,
            Integer page,
            Integer pageSize,
            CurrentUser admin
    ) {
        requirePermission(admin, "admin:orders:read");
        if (paymentOrderMapper.selectById(orderId) == null) {
            throw BusinessException.notFound("订单不存在");
        }
        int currentPage = page == null ? 1 : page;
        int currentPageSize = pageSize == null ? 20 : pageSize;
        Page<AdminOperationLog> result = adminOperationLogMapper.selectTargetLogPage(
                Page.of(currentPage, currentPageSize),
                "payment_order",
                orderId
        );
        return PageResult.of(
                result.getRecords().stream().map(this::toOperationLogVO).toList(),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    @Override
    @Transactional
    public AdminPaymentOrderVO markOrderFailed(
            Long orderId,
            AdminFailPaymentOrderDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:orders:update");
        PaymentOrder order = paymentOrderMapper.selectById(orderId);
        if (order == null) {
            throw BusinessException.notFound("订单不存在");
        }
        if (!"pending".equals(order.getStatus())) {
            throw BusinessException.conflict("只有待支付订单可以标记为失败");
        }

        OffsetDateTime now = OffsetDateTime.now();
        int updated = paymentOrderMapper.markPendingOrderFailed(orderId, now);
        if (updated == 0) {
            throw BusinessException.conflict("订单状态已变化，请刷新后重试");
        }
        String beforeStatus = order.getStatus();
        order.setStatus("failed");
        order.setUpdatedAt(now);
        writeOperationLog(admin.id(), "payment.order.mark_failed", "payment_order", orderId, Map.of(
                "orderNo", order.getOrderNo(),
                "beforeStatus", beforeStatus,
                "afterStatus", "failed",
                "reason", request.reason().trim()
        ), ipAddress);
        return toOrderVOs(List.of(order), pendingTimeoutCutoff(null)).get(0);
    }

    @Override
    public PageResult<AdminPaymentNotificationVO> pagePaymentNotifications(
            AdminPaymentNotificationQueryDTO query,
            CurrentUser admin
    ) {
        requirePermission(admin, "admin:orders:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : "";
        String resultCode = StringUtils.hasText(query.getResultCode()) ? query.getResultCode().trim() : "";
        query.setResultCode(resultCode);
        List<Long> keywordOrderIds = StringUtils.hasText(keyword) ? findOrderIdsByNotificationKeyword(keyword) : List.of();
        Long numericOrderId = parseLong(keyword);
        Page<PaymentNotification> result = paymentNotificationMapper.selectNotificationPage(
                Page.of(page, pageSize),
                query,
                keyword,
                numericOrderId,
                keywordOrderIds
        );
        return PageResult.of(
                toNotificationVOs(result.getRecords()),
                result.getTotal(),
                result.getCurrent(),
                result.getSize()
        );
    }

    private LambdaQueryWrapper<PaymentOrder> buildOrderWrapper(AdminPaymentOrderQueryDTO query, OffsetDateTime pendingCutoff) {
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
        applyOrderExceptionFilter(wrapper, query.getExceptionType(), pendingCutoff);
        return wrapper;
    }

    private void applyOrderExceptionFilter(
            LambdaQueryWrapper<PaymentOrder> wrapper,
            String exceptionType,
            OffsetDateTime pendingCutoff
    ) {
        if (!StringUtils.hasText(exceptionType)) {
            return;
        }
        if (EXCEPTION_PENDING_TIMEOUT.equals(exceptionType)) {
            wrapper.eq(PaymentOrder::getStatus, "pending")
                    .le(PaymentOrder::getCreatedAt, pendingCutoff);
            return;
        }
        if (EXCEPTION_CALLBACK_FAILED.equals(exceptionType)) {
            wrapper.apply("""
                    exists (
                      select 1
                      from payment_notifications notification
                      where notification.order_id = payment_orders.id
                        and notification.process_status = 'failed'
                    )
                    """);
            return;
        }
        if (EXCEPTION_AMOUNT_MISMATCH.equals(exceptionType)) {
            wrapper.apply("""
                    exists (
                      select 1
                      from payment_notifications notification
                      where notification.order_id = payment_orders.id
                        and notification.process_status = 'failed'
                        and notification.result_code = 'AMOUNT_MISMATCH'
                    )
                    """);
            return;
        }
        if (EXCEPTION_PROVIDER_MISMATCH.equals(exceptionType)) {
            wrapper.apply("""
                    exists (
                      select 1
                      from payment_notifications notification
                      where notification.order_id = payment_orders.id
                        and notification.process_status = 'failed'
                        and notification.result_code = 'PROVIDER_MISMATCH'
                    )
                    """);
            return;
        }
        if (EXCEPTION_MEMBERSHIP_MISSING.equals(exceptionType)) {
            wrapper.eq(PaymentOrder::getStatus, "paid")
                    .notExists("""
                            select 1
                            from user_memberships membership
                            where membership.payment_order_id = payment_orders.id
                            """);
            return;
        }
        if (EXCEPTION_ALL.equals(exceptionType)) {
            wrapper.apply("""
                    (
                      (
                        status = 'pending'
                        and created_at <= {0}
                      )
                      or exists (
                        select 1
                        from payment_notifications notification
                        where notification.order_id = payment_orders.id
                          and notification.process_status = 'failed'
                      )
                      or (
                        status = 'paid'
                        and not exists (
                          select 1
                          from user_memberships membership
                          where membership.payment_order_id = payment_orders.id
                        )
                      )
                    )
                    """, pendingCutoff);
        }
    }

    private List<AdminPaymentOrderVO> toOrderVOs(List<PaymentOrder> orders, OffsetDateTime pendingCutoff) {
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, User> users = loadUsers(orders);
        Map<Long, MembershipPlan> plans = loadPlans(orders);
        Map<Long, PaymentNotification> latestNotifications = loadLatestNotifications(orders);
        Map<Long, Set<String>> failedResultCodes = loadFailedResultCodes(orders);
        Set<Long> membershipOrderIds = loadMembershipOrderIds(orders);
        return orders.stream()
                .map(order -> {
                    User user = users.get(order.getUserId());
                    MembershipPlan plan = plans.get(order.getPlanId());
                    PaymentNotification latestNotification = latestNotifications.get(order.getId());
                    List<String> exceptionTypes = resolveOrderExceptionTypes(
                            order,
                            failedResultCodes.getOrDefault(order.getId(), Set.of()),
                            membershipOrderIds.contains(order.getId()),
                            pendingCutoff
                    );
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
                            exceptionTypes,
                            latestNotification == null ? null : latestNotification.getProcessStatus(),
                            latestNotification == null ? null : latestNotification.getResultCode(),
                            latestNotification == null ? null : latestNotification.getResultMessage(),
                            latestNotification == null ? null : latestNotification.getReceivedAt(),
                            order.getPaidAt(),
                            order.getCreatedAt(),
                            order.getUpdatedAt()
                    );
                })
                .toList();
    }

    private List<AdminPaymentNotificationVO> toNotificationVOs(List<PaymentNotification> notifications) {
        if (notifications.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, PaymentOrder> orders = loadOrdersByNotification(notifications);
        Map<Long, User> users = loadUsers(orders.values().stream().toList());
        Map<Long, MembershipPlan> plans = loadPlans(orders.values().stream().toList());
        return notifications.stream()
                .map(notification -> {
                    PaymentOrder order = orders.get(notification.getOrderId());
                    User user = order == null ? null : users.get(order.getUserId());
                    MembershipPlan plan = order == null ? null : plans.get(order.getPlanId());
                    return new AdminPaymentNotificationVO(
                            notification.getId(),
                            notification.getOrderId(),
                            order == null ? null : order.getOrderNo(),
                            order == null ? null : order.getUserId(),
                            user == null ? null : user.getEmail(),
                            user == null ? null : user.getNickname(),
                            order == null ? null : order.getPlanId(),
                            plan == null ? null : plan.getName(),
                            order == null ? null : order.getAmount(),
                            order == null ? null : order.getCurrency(),
                            order == null ? null : order.getStatus(),
                            notification.getProvider(),
                            notification.getProviderTradeNo(),
                            notification.getSignatureValid(),
                            notification.getHandled(),
                            notification.getProcessStatus(),
                            notification.getResultCode(),
                            notification.getResultMessage(),
                            notification.getNotifyPayload(),
                            notification.getReceivedAt(),
                            notification.getCreatedAt(),
                            notification.getUpdatedAt()
                    );
                })
                .toList();
    }

    private List<Long> findOrderIdsByNotificationKeyword(String keyword) {
        Set<Long> orderIds = new LinkedHashSet<>();
        Long numericId = parseLong(keyword);
        if (numericId != null) {
            orderIds.add(numericId);
        }

        List<Long> userIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .like(User::getEmail, keyword)
                        .or()
                        .like(User::getNickname, keyword))
                .stream()
                .map(User::getId)
                .toList();
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(item -> {
            item.like(PaymentOrder::getOrderNo, keyword)
                    .or()
                    .like(PaymentOrder::getProviderTradeNo, keyword);
            if (!userIds.isEmpty()) {
                item.or().in(PaymentOrder::getUserId, userIds);
            }
        });
        paymentOrderMapper.selectList(wrapper).stream()
                .map(PaymentOrder::getId)
                .filter(Objects::nonNull)
                .forEach(orderIds::add);
        return orderIds.stream().toList();
    }

    private User resolveOfflinePaymentUser(String userKeyword) {
        String keyword = userKeyword == null ? "" : userKeyword.trim();
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "请选择用户");
        }
        Long userId = parseLong(keyword);
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                throw BusinessException.notFound("用户不存在");
            }
            return user;
        }
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, keyword)
                .or()
                .eq(User::getNickname, keyword)
                .last("limit 2"));
        if (users.isEmpty()) {
            throw BusinessException.notFound("用户不存在");
        }
        if (users.size() > 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "匹配到多个用户，请使用用户 ID 或邮箱");
        }
        return users.get(0);
    }

    private Map<Long, PaymentNotification> loadLatestNotifications(List<PaymentOrder> orders) {
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return paymentNotificationMapper.selectLatestByOrderIds(orderIds)
                .stream()
                .filter(notification -> notification.getOrderId() != null)
                .collect(Collectors.toMap(PaymentNotification::getOrderId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, Set<String>> loadFailedResultCodes(List<PaymentOrder> orders) {
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return paymentNotificationMapper.selectFailedResultsByOrderIds(orderIds)
                .stream()
                .filter(notification -> notification.getOrderId() != null)
                .collect(Collectors.groupingBy(
                        PaymentNotification::getOrderId,
                        Collectors.mapping(PaymentNotification::getResultCode, Collectors.toSet())
                ));
    }

    private Set<Long> loadMembershipOrderIds(List<PaymentOrder> orders) {
        List<Long> orderIds = orderIds(orders);
        if (orderIds.isEmpty()) {
            return Collections.emptySet();
        }
        return userMembershipMapper.selectList(new LambdaQueryWrapper<UserMembership>()
                        .in(UserMembership::getPaymentOrderId, orderIds))
                .stream()
                .map(UserMembership::getPaymentOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<String> resolveOrderExceptionTypes(
            PaymentOrder order,
            Set<String> failedResultCodes,
            boolean hasMembership,
            OffsetDateTime pendingCutoff
    ) {
        List<String> exceptionTypes = new java.util.ArrayList<>();
        if ("pending".equals(order.getStatus())
                && order.getCreatedAt() != null
                && !order.getCreatedAt().isAfter(pendingCutoff)) {
            exceptionTypes.add(EXCEPTION_PENDING_TIMEOUT);
        }
        if (!failedResultCodes.isEmpty()) {
            exceptionTypes.add(EXCEPTION_CALLBACK_FAILED);
        }
        if (failedResultCodes.contains(RESULT_AMOUNT_MISMATCH)) {
            exceptionTypes.add(EXCEPTION_AMOUNT_MISMATCH);
        }
        if (failedResultCodes.contains(RESULT_PROVIDER_MISMATCH)) {
            exceptionTypes.add(EXCEPTION_PROVIDER_MISMATCH);
        }
        if ("paid".equals(order.getStatus()) && !hasMembership) {
            exceptionTypes.add(EXCEPTION_MEMBERSHIP_MISSING);
        }
        return exceptionTypes;
    }

    private List<Long> orderIds(List<PaymentOrder> orders) {
        return orders.stream()
                .map(PaymentOrder::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Long, PaymentOrder> loadOrdersByNotification(List<PaymentNotification> notifications) {
        List<Long> orderIds = notifications.stream()
                .map(PaymentNotification::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return paymentOrderMapper.selectBatchIds(orderIds)
                .stream()
                .collect(Collectors.toMap(PaymentOrder::getId, Function.identity()));
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

    private OffsetDateTime pendingTimeoutCutoff(Integer pendingTimeoutMinutes) {
        int minutes = pendingTimeoutMinutes == null ? DEFAULT_PENDING_TIMEOUT_MINUTES : pendingTimeoutMinutes;
        return OffsetDateTime.now().minusMinutes(minutes);
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
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

    private AdminOperationLogVO toOperationLogVO(AdminOperationLog log) {
        return new AdminOperationLogVO(
                log.getId(),
                log.getAdminUserId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getIpAddress(),
                log.getCreatedAt(),
                log.getUpdatedAt()
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

    private void evictMembershipPlanCache() {
        masterDataCache.evictByPrefix("membership:plans:");
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
