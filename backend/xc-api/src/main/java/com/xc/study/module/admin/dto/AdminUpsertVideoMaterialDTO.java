package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUpsertVideoMaterialDTO(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Pattern(regexp = "drama|short_video|cartoon") String materialType,
        @Size(max = 4000) String description,
        Long coverAssetId,
        @Pattern(regexp = "active|inactive") String status
) {
}
