package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminUserDetailVO(
        Long id,
        String email,
        String nickname,
        String status,
        String accessLevel,
        boolean fullAccess,
        OffsetDateTime trialStartedAt,
        OffsetDateTime trialEndsAt,
        OffsetDateTime membershipEndsAt,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        AdminLearningSummaryVO learningSummary,
        List<AdminMembershipRecordVO> memberships
) {
}
