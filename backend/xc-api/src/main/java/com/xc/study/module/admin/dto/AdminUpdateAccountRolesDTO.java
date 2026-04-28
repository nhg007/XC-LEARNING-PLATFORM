package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AdminUpdateAccountRolesDTO(
        @NotNull List<Long> roleIds
) {
}
