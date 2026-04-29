package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminUpdateAccountRolesDTO(
        @NotNull @NotEmpty(message = "请选择角色") List<Long> roleIds
) {
}
