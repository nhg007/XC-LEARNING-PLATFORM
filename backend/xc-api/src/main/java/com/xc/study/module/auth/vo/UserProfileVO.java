package com.xc.study.module.auth.vo;

import java.time.OffsetDateTime;

public record UserProfileVO(
        Long id,
        String email,
        String nickname,
        String status,
        OffsetDateTime trialStartedAt,
        OffsetDateTime trialEndsAt
) {
}
