package com.xc.study.module.admin.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminRoleVO(
        Long id,
        String roleCode,
        String roleName,
        String description,
        List<AdminPermissionVO> permissions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
