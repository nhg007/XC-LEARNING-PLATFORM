package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminExerciseSetQueryDTO;
import com.xc.study.module.admin.dto.AdminSentenceExerciseQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertExerciseSetDTO;
import com.xc.study.module.admin.dto.AdminUpsertSentenceExerciseDTO;
import com.xc.study.module.admin.service.AdminExerciseManagementService;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminExerciseSetVO;
import com.xc.study.module.admin.vo.AdminSentenceExerciseVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin")
public class AdminExerciseManagementController {

    private final AdminExerciseManagementService adminExerciseManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminExerciseManagementController(
            AdminExerciseManagementService adminExerciseManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminExerciseManagementService = adminExerciseManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/exercise-sets")
    public ApiResponse<PageResult<AdminExerciseSetVO>> pageSets(@Valid AdminExerciseSetQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.pageSets(query, admin));
    }

    @PostMapping("/exercise-sets")
    public ApiResponse<AdminExerciseSetVO> createSet(
            @Valid @RequestBody AdminUpsertExerciseSetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.createSet(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/exercise-sets/{setId}")
    public ApiResponse<AdminExerciseSetVO> updateSet(
            @PathVariable Long setId,
            @Valid @RequestBody AdminUpsertExerciseSetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.updateSet(setId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/exercise-sets/{setId}/status")
    public ApiResponse<AdminExerciseSetVO> updateSetStatus(
            @PathVariable Long setId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.updateSetStatus(setId, request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/sentence-exercises")
    public ApiResponse<PageResult<AdminSentenceExerciseVO>> pageSentenceExercises(@Valid AdminSentenceExerciseQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.pageSentenceExercises(query, admin));
    }

    @PostMapping("/sentence-exercises")
    public ApiResponse<AdminSentenceExerciseVO> createSentenceExercise(
            @Valid @RequestBody AdminUpsertSentenceExerciseDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.createSentenceExercise(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/sentence-exercises/{exerciseId}")
    public ApiResponse<AdminSentenceExerciseVO> updateSentenceExercise(
            @PathVariable Long exerciseId,
            @Valid @RequestBody AdminUpsertSentenceExerciseDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.updateSentenceExercise(exerciseId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/sentence-exercises/{exerciseId}/status")
    public ApiResponse<AdminSentenceExerciseVO> updateSentenceExerciseStatus(
            @PathVariable Long exerciseId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.updateSentenceExerciseStatus(exerciseId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/sentence-exercises/audio-bindings")
    public ApiResponse<AdminBatchBindMediaAssetResultVO> bindSentenceExerciseAudio(
            @Valid @RequestBody AdminBatchBindMediaAssetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminExerciseManagementService.bindSentenceExerciseAudio(request, admin, clientIp(servletRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
