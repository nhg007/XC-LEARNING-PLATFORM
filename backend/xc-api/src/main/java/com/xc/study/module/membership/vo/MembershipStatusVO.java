package com.xc.study.module.membership.vo;

import java.time.OffsetDateTime;

public record MembershipStatusVO(
        String accessLevel,
        boolean fullAccess,
        OffsetDateTime trialEndsAt,
        OffsetDateTime membershipEndsAt,
        long remainingSeconds
) {
}
