package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpdateClassRoomStatusDTO(
        @NotBlank @Pattern(regexp = "active|archived") String status,
        @Size(max = 1000) String reason
) {
}
