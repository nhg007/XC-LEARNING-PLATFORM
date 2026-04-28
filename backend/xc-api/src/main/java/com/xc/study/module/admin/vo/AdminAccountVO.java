package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAccountVO(
        Long id,
        String username,
        String displayName,
        String status,
        OffsetDateTime lastLoginAt,
        List<AdminRoleVO> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
