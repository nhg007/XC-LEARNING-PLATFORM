package com.xc.study.module.admin.vo;

public record AdminPermissionVO(
        Long id,
        String permissionCode,
        String permissionName,
        String moduleName
) {
}
