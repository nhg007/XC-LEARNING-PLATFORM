package com.xc.study.module.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClassRoomRequest(
        @NotBlank @Size(max = 64) String inviteCode
) {
}
