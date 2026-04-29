package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminClassRoomListItemVO(
        Long id,
        String name,
        String description,
        String inviteCode,
        String status,
        Long teacherAdminUserId,
        String teacherUsername,
        String teacherDisplayName,
        long activeMemberCount,
        long pendingMemberCount,
        Integer totalStudySeconds,
        Integer totalExerciseCount,
        Integer totalCorrectCount,
        BigDecimal accuracyRate,
        OffsetDateTime lastStudyAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
