package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminUpdateClassRoomDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description
) {
}
