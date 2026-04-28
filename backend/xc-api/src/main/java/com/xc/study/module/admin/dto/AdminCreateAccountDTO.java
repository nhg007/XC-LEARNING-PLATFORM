package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminCreateAccountDTO(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_.@-]+") String username,
        @Size(max = 100) String displayName,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Pattern(regexp = "active|disabled") String status,
        @NotNull List<Long> roleIds
) {
}
