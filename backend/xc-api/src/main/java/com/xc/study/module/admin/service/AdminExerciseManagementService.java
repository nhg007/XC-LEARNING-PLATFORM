package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminExerciseSetQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceExerciseQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertExerciseSetDTO;
import com.xc.study.module.admin.dto.AdminUpsertSentenceExerciseDTO;
import com.xc.study.module.admin.vo.AdminExerciseSetVO;
import com.xc.study.module.admin.vo.AdminSentenceExerciseVO;
import com.xc.study.security.CurrentUser;

public interface AdminExerciseManagementService {

    PageResult<AdminExerciseSetVO> pageSets(AdminExerciseSetQueryDTO query, CurrentUser admin);

    AdminExerciseSetVO createSet(AdminUpsertExerciseSetDTO request, CurrentUser admin, String ipAddress);

    AdminExerciseSetVO updateSet(Long setId, AdminUpsertExerciseSetDTO request, CurrentUser admin, String ipAddress);

    AdminExerciseSetVO updateSetStatus(Long setId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminSentenceExerciseVO> pageSentenceExercises(AdminSentenceExerciseQueryDTO query, CurrentUser admin);

    AdminSentenceExerciseVO createSentenceExercise(AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress);

    AdminSentenceExerciseVO updateSentenceExercise(Long exerciseId, AdminUpsertSentenceExerciseDTO request, CurrentUser admin, String ipAddress);

    AdminSentenceExerciseVO updateSentenceExerciseStatus(Long exerciseId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);
}
