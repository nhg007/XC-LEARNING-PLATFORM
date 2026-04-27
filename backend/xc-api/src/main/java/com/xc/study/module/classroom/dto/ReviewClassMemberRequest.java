package com.xc.study.module.classroom.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewClassMemberRequest(
        @NotNull Boolean approved
) {
}
