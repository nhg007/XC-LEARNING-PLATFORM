package com.xc.study.module.admin.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminCreateClassRoomDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @Positive Long teacherAdminUserId,
        @Size(max = 255) String teacherAdminUsername
) {

    @AssertTrue(message = "teacherAdminUserId 或 teacherAdminUsername 不能为空")
    public boolean isTeacherProvided() {
        return teacherAdminUserId != null || (teacherAdminUsername != null && !teacherAdminUsername.isBlank());
    }
}
