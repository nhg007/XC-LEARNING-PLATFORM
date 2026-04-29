package com.xc.study.module.admin.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminClassRoomDetailVO(
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
        long removedMemberCount,
        Integer totalStudySeconds,
        Integer totalExerciseCount,
        Integer totalCorrectCount,
        BigDecimal accuracyRate,
        OffsetDateTime lastStudyAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<AdminClassMemberVO> members,
        List<AdminClassMemberStatsVO> stats
) {
}
