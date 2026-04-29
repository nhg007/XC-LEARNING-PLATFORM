package com.xc.study.module.membership.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.xc.study.common.BusinessException;
import com.xc.study.common.cache.MasterDataCache;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.membership.entity.MembershipPlan;
import com.xc.study.module.membership.entity.UserMembership;
import com.xc.study.module.membership.mapper.MembershipPlanMapper;
import com.xc.study.module.membership.mapper.UserMembershipMapper;
import com.xc.study.module.membership.vo.MembershipPlanVO;
import com.xc.study.module.membership.vo.MembershipStatusVO;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {

    private static final TypeReference<List<MembershipPlanVO>> ACTIVE_PLAN_LIST_TYPE = new TypeReference<>() {
    };

    private final UserMapper userMapper;
    private final UserMembershipMapper userMembershipMapper;
    private final MembershipPlanMapper membershipPlanMapper;
    private final MasterDataCache masterDataCache;

    public MembershipService(
            UserMapper userMapper,
            UserMembershipMapper userMembershipMapper,
            MembershipPlanMapper membershipPlanMapper,
            MasterDataCache masterDataCache
    ) {
        this.userMapper = userMapper;
        this.userMembershipMapper = userMembershipMapper;
        this.membershipPlanMapper = membershipPlanMapper;
        this.masterDataCache = masterDataCache;
    }

    public MembershipStatusVO getStatus(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }

        OffsetDateTime now = OffsetDateTime.now();
        UserMembership membership = userMembershipMapper.selectOne(new LambdaQueryWrapper<UserMembership>()
                .eq(UserMembership::getUserId, userId)
                .eq(UserMembership::getStatus, "active")
                .le(UserMembership::getStartedAt, now)
                .gt(UserMembership::getEndsAt, now)
                .orderByDesc(UserMembership::getEndsAt)
                .last("limit 1"));
        if (membership != null) {
            return new MembershipStatusVO(
                    "member",
                    true,
                    user.getTrialEndsAt(),
                    membership.getEndsAt(),
                    remainingSeconds(now, membership.getEndsAt())
            );
        }

        if (user.getTrialEndsAt() != null && user.getTrialEndsAt().isAfter(now)) {
            return new MembershipStatusVO(
                    "trial",
                    true,
                    user.getTrialEndsAt(),
                    null,
                    remainingSeconds(now, user.getTrialEndsAt())
            );
        }

        return new MembershipStatusVO("free", false, user.getTrialEndsAt(), null, 0);
    }

    public void requireFullAccess(Long userId) {
        if (!getStatus(userId).fullAccess()) {
            throw BusinessException.forbidden(ErrorCode.MEMBERSHIP_REQUIRED, "当前功能需要试用或会员权限");
        }
    }

    public List<MembershipPlanVO> listActivePlans() {
        return masterDataCache.get("membership:plans:active", ACTIVE_PLAN_LIST_TYPE, this::loadActivePlans);
    }

    private List<MembershipPlanVO> loadActivePlans() {
        return membershipPlanMapper.selectList(new LambdaQueryWrapper<MembershipPlan>()
                        .eq(MembershipPlan::getStatus, "active")
                        .orderByAsc(MembershipPlan::getPrice)
                        .orderByAsc(MembershipPlan::getDurationDays))
                .stream()
                .map(this::toPlanVO)
                .toList();
    }

    private MembershipPlanVO toPlanVO(MembershipPlan plan) {
        return new MembershipPlanVO(
                plan.getId(),
                plan.getName(),
                plan.getDurationDays(),
                plan.getDurationUnit(),
                plan.getDurationValue(),
                plan.getPrice(),
                plan.getCurrency()
        );
    }

    private long remainingSeconds(OffsetDateTime now, OffsetDateTime endsAt) {
        return Math.max(0, Duration.between(now, endsAt).toSeconds());
    }
}
