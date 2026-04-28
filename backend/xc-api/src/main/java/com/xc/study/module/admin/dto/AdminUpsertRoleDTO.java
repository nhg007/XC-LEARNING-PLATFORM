package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpsertRoleDTO(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[a-z][a-z0-9_:-]*") String roleCode,
        @NotBlank @Size(max = 100) String roleName,
        @Size(max = 2000) String description
) {
}
