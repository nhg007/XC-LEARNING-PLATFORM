package com.xc.study.module.classroom.vo;

public record ClassRoomVO(
        Long id,
        String name,
        String description,
        String inviteCode,
        String ownerName,
        String ownerContact,
        Long ownerUserId,
        boolean createdByMe,
        String status,
        String memberStatus
) {
}
