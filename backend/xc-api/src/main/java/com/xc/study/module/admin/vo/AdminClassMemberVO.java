package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;

public record AdminClassMemberVO(
        Long id,
        Long classId,
        Long userId,
        String email,
        String nickname,
        String userStatus,
        String memberRole,
        String status,
        Long invitedByUserId,
        Long reviewedByUserId,
        OffsetDateTime reviewedAt,
        OffsetDateTime joinedAt,
        OffsetDateTime removedAt,
        OffsetDateTime createdAt
) {
}
