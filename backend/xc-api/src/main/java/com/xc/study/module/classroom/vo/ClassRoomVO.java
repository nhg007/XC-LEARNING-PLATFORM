package com.xc.study.module.classroom.vo;

public record ClassRoomVO(
        Long id,
        String name,
        String description,
        String inviteCode,
        String status,
        String memberRole,
        String memberStatus
) {
}
