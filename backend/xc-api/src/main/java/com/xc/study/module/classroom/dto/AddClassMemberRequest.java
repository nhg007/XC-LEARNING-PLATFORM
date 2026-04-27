package com.xc.study.module.classroom.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AddClassMemberRequest(
        Long userId,
        @Email @Size(max = 255) String email
) {
    @AssertTrue(message = "userId 或 email 不能为空")
    public boolean hasTarget() {
        return userId != null || (email != null && !email.isBlank());
    }
}
