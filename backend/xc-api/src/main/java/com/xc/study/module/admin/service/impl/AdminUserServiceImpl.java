package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAdjustMembershipDTO;
import com.xc.study.module.admin.dto.AdminUpdateUserStatusDTO;
import com.xc.study.module.admin.dto.AdminUserQueryDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.service.AdminUserService;
import com.xc.study.module.admin.vo.AdminLearningSummaryVO;
import com.xc.study.module.admin.vo.AdminMembershipRecordVO;
import com.xc.study.module.admin.vo.AdminUserDetailVO;
import com.xc.study.module.admin.vo.AdminUserListItemVO;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.stats.entity.UserLearningSummary;
import com.xc.study.module.stats.mapper.UserLearningSummaryMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.security.CurrentUser;
import java.math.BigDecimal;
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
public class AdminUserServiceImpl implements AdminUserService {

    private static final String ACCESS_MEMBER = "member";
    private static final String ACCESS_TRIAL = "trial";
    private static final String ACCESS_FREE = "free";

    private final UserMapper userMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final UserLearningSummaryMapper userLearningSummaryMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;

    public AdminUserServiceImpl(
            UserMapper userMapper,
            UserMembershipMapper userMembershipMapper,
            MembershipPlanMapper membershipPlanMapper,
            UserLearningSummaryMapper userLearningSummaryMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper
    ) {
        this.userMapper = userMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.membershipPlanMapper = membershipPlanMapper;
        this.userLearningSummaryMapper = userLearningSummaryMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public PageResult<AdminUserListItemVO> pageUsers(AdminUserQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:users:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(User::getStatus, query.getStatus());
        } else {
            wrapper.ne(User::getStatus, "deleted");
        }
        wrapper.notExists("""
                select 1
                from class_members member
                where member.user_id = users.id
                  and member.member_role = 'teacher'
                """);
        wrapper.notExists("""
                select 1
                from admin_users admin_user
                where lower(admin_user.username) = lower(users.email)
                """);
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(User::getEmail, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<User> users = result.getRecords();
        List<Long> userIds = users.stream().map(User::getId).toList();
        OffsetDateTime now = OffsetDateTime.now();
        Map<Long, UserMembership> activeMemberships = loadActiveMemberships(userIds, now);
        Map<Long, UserLearningSummary> summaries = loadLearningSummaries(userIds);

        List<AdminUserListItemVO> records = users.stream()
                .map(user -> toListItem(user, activeMemberships.get(user.getId()), summaries.get(user.getId()), now))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public AdminUserDetailVO getUserDetail(Long userId, CurrentUser admin) {
        requirePermission(admin, "admin:users:read");
        return buildUserDetail(requireUser(userId));
    }

    @Override
    @Transactional
    public AdminUserDetailVO updateUserStatus(Long userId, AdminUpdateUserStatusDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:users:update");
        User user = requireUser(userId);
        String beforeStatus = user.getStatus();
        if ("deleted".equals(beforeStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "已删除用户不能调整状态");
        }
        user.setStatus(request.status());
        user.setUpdatedAt(OffsetDateTime.now());
        userMapper.updateById(user);

        writeOperationLog(admin.id(), "user.status.update", "user", userId, Map.of(
                "beforeStatus", beforeStatus,
                "afterStatus", request.status(),
                "reason", nullToEmpty(request.reason())
        ), ipAddress);
        return buildUserDetail(userMapper.selectById(userId));
    }

    @Override
    @Transactional
    public AdminUserDetailVO adjustMembership(Long userId, AdminAdjustMembershipDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:memberships:adjust");
        User user = requireUser(userId);
        if ("deleted".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已删除用户不能调整会员");
        }
        if ("grant".equals(request.action())) {
            grantMembership(userId, request, admin.id());
        } else {
            cancelActiveMemberships(userId, OffsetDateTime.now());
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", request.action());
        detail.put("startedAt", request.startedAt());
        detail.put("endsAt", request.endsAt());
        detail.put("reason", request.reason());
        writeOperationLog(admin.id(), "user.membership." + request.action(), "user", userId, detail, ipAddress);
        return buildUserDetail(userMapper.selectById(userId));
    }

    private AdminUserListItemVO toListItem(
            User user,
            UserMembership membership,
            UserLearningSummary summary,
            OffsetDateTime now
    ) {
        AccessSnapshot access = accessSnapshot(user, membership, now);
        AdminLearningSummaryVO learningSummary = toLearningSummary(summary);
        return new AdminUserListItemVO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                access.accessLevel(),
                access.fullAccess(),
                user.getTrialEndsAt(),
                access.membershipEndsAt(),
                learningSummary.totalStudySeconds(),
                learningSummary.totalExerciseCount(),
                learningSummary.overallAccuracyRate(),
                learningSummary.currentStreakDays(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private AdminUserDetailVO buildUserDetail(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        UserMembership activeMembership = loadActiveMembership(user.getId(), now);
        AccessSnapshot access = accessSnapshot(user, activeMembership, now);
        UserLearningSummary summary = userLearningSummaryMapper.selectOne(new LambdaQueryWrapper<UserLearningSummary>()
                .eq(UserLearningSummary::getUserId, user.getId())
                .last("limit 1"));
        List<AdminMembershipRecordVO> memberships = loadMembershipRecords(user.getId());
        return new AdminUserDetailVO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                access.accessLevel(),
                access.fullAccess(),
                user.getTrialStartedAt(),
                user.getTrialEndsAt(),
                access.membershipEndsAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                toLearningSummary(summary),
                memberships
        );
    }

    private List<AdminMembershipRecordVO> loadMembershipRecords(Long userId) {
        List<UserMembership> memberships = userMembershipMapper.selectList(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId)
                .orderByDesc(UserMembership::getCreatedAt)
                .last("limit 20"));
        List<Long> planIds = memberships.stream()
                .map(UserMembership::getPlanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MembershipPlan> plans = planIds.isEmpty()
                ? Collections.emptyMap()
                : membershipPlanMapper.selectBatchIds(planIds)
                        .stream()
                        .collect(Collectors.toMap(MembershipPlan::getId, Function.identity()));
        return memberships.stream()
                .map(item -> {
                    MembershipPlan plan = plans.get(item.getPlanId());
                    return new AdminMembershipRecordVO(
                            item.getId(),
                            item.getPlanId(),
                            plan == null ? null : plan.getName(),
                            item.getStartedAt(),
                            item.getEndsAt(),
                            item.getSource(),
                            item.getAdjustedByAdminId(),
                            item.getAdjustReason(),
                            item.getStatus(),
                            item.getCreatedAt()
                    );
                })
                .toList();
    }

    private Map<Long, UserMembership> loadActiveMemberships(List<Long> userIds, OffsetDateTime now) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<UserMembership> memberships = userMembershipMapper.selectList(new LambdaQueryWrapper<UserMembership>()
                .in(UserMembership::getUserId, userIds)
                .eq(UserMembership::getStatus, "active")
                .le(UserMembership::getStartedAt, now)
                .gt(UserMembership::getEndsAt, now)
                .orderByDesc(UserMembership::getEndsAt));
        Map<Long, UserMembership> result = new LinkedHashMap<>();
        memberships.forEach(item -> result.putIfAbsent(item.getUserId(), item));
        return result;
    }

    private UserMembership loadActiveMembership(Long userId, OffsetDateTime now) {
        return userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId)
                .eq(UserMembership::getStatus, "active")
                .le(UserMembership::getStartedAt, now)
                .gt(UserMembership::getEndsAt, now)
                .orderByDesc(UserMembership::getEndsAt)
                .last("limit 1"));
    }

    private Map<Long, UserLearningSummary> loadLearningSummaries(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userLearningSummaryMapper.selectList(new LambdaQueryWrapper<UserLearningSummary>()
                        .in(UserLearningSummary::getUserId, userIds))
                .stream()
                .collect(Collectors.toMap(UserLearningSummary::getUserId, Function.identity(), (left, right) -> left));
    }

    private AdminLearningSummaryVO toLearningSummary(UserLearningSummary summary) {
        if (summary == null) {
            return new AdminLearningSummaryVO(0, 0, 0, 0, 0, 0, BigDecimal.ZERO, null);
        }
        return new AdminLearningSummaryVO(
                valueOrZero(summary.getTotalStudySeconds()),
                valueOrZero(summary.getTotalExerciseCount()),
                valueOrZero(summary.getTotalCorrectCount()),
                valueOrZero(summary.getTotalVocabReviewCount()),
                valueOrZero(summary.getCurrentStreakDays()),
                valueOrZero(summary.getLongestStreakDays()),
                summary.getOverallAccuracyRate() == null ? BigDecimal.ZERO : summary.getOverallAccuracyRate(),
                summary.getLastStudyDate()
        );
    }

    private void grantMembership(Long userId, AdminAdjustMembershipDTO request, Long adminId) {
        OffsetDateTime startedAt = request.startedAt() == null ? OffsetDateTime.now() : request.startedAt();
        OffsetDateTime endsAt = request.endsAt();
        if (endsAt == null || !endsAt.isAfter(startedAt)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "会员结束时间必须晚于开始时间");
        }
        cancelActiveMemberships(userId, OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        UserMembership membership = new UserMembership();
        membership.setUserId(userId);
        membership.setStartedAt(startedAt);
        membership.setEndsAt(endsAt);
        membership.setSource("admin_adjustment");
        membership.setAdjustedByAdminId(adminId);
        membership.setAdjustReason(request.reason());
        membership.setStatus("active");
        membership.setCreatedAt(now);
        membership.setUpdatedAt(now);
        userMembershipMapper.insert(membership);
    }

    private void cancelActiveMemberships(Long userId, OffsetDateTime now) {
        List<UserMembership> memberships = userMembershipMapper.selectList(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId)
                .eq(UserMembership::getStatus, "active")
                .gt(UserMembership::getEndsAt, now));
        memberships.forEach(item -> {
            item.setStatus("cancelled");
            item.setUpdatedAt(now);
            userMembershipMapper.updateById(item);
        });
    }

    private AccessSnapshot accessSnapshot(User user, UserMembership membership, OffsetDateTime now) {
        if (membership != null) {
            return new AccessSnapshot(ACCESS_MEMBER, true, membership.getEndsAt());
        }
        if (user.getTrialEndsAt() != null && user.getTrialEndsAt().isAfter(now)) {
            return new AccessSnapshot(ACCESS_TRIAL, true, null);
        }
        return new AccessSnapshot(ACCESS_FREE, false, null);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
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
        Map<String, Object> safeDetail = new HashMap<>(detail);
        try {
            return objectMapper.writeValueAsString(safeDetail);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private Integer valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record AccessSnapshot(String accessLevel, boolean fullAccess, OffsetDateTime membershipEndsAt) {
    }
}
