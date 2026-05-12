package com.xc.study.module.classroom.vo;

import java.time.OffsetDateTime;

public record ClassMemberVO(
        Long id,
        Long classId,
        Long userId,
        String email,
        String nickname,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime removedAt
) {
}
