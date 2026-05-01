package com.xc.study.module.classroom.vo;

public record ClassRoomDetailVO(
        Long id,
        String name,
        String description,
        String inviteCode,
        String teacherName,
        String teacherContact,
        String status,
        String memberRole,
        String memberStatus,
        long activeMemberCount,
        long pendingMemberCount
) {
}
