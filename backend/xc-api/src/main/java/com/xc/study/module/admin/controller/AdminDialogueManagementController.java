package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminDialogueLineQueryDTO;
import com.xc.study.module.admin.dto.AdminDialogueLineVocabQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineVocabDTO;
import com.xc.study.module.admin.dto.AdminUpsertVideoMaterialDTO;
import com.xc.study.module.admin.dto.AdminVideoMaterialQueryDTO;
import com.xc.study.module.admin.service.AdminDialogueManagementService;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminDialogueLineVO;
import com.xc.study.module.admin.vo.AdminDialogueLineVocabVO;
import com.xc.study.module.admin.vo.AdminVideoMaterialVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class AdminDialogueManagementController {

    private final AdminDialogueManagementService adminDialogueManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminDialogueManagementController(
            AdminDialogueManagementService adminDialogueManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminDialogueManagementService = adminDialogueManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/video-materials")
    public ApiResponse<PageResult<AdminVideoMaterialVO>> pageMaterials(@Valid AdminVideoMaterialQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.pageMaterials(query, admin));
    }

    @PostMapping("/video-materials")
    public ApiResponse<AdminVideoMaterialVO> createMaterial(
            @Valid @RequestBody AdminUpsertVideoMaterialDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.createMaterial(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/video-materials/{materialId}")
    public ApiResponse<AdminVideoMaterialVO> updateMaterial(
            @PathVariable Long materialId,
            @Valid @RequestBody AdminUpsertVideoMaterialDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.updateMaterial(materialId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/video-materials/{materialId}/status")
    public ApiResponse<AdminVideoMaterialVO> updateMaterialStatus(
            @PathVariable Long materialId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.updateMaterialStatus(materialId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/video-materials/cover-bindings")
    public ApiResponse<AdminBatchBindMediaAssetResultVO> bindMaterialCover(
            @Valid @RequestBody AdminBatchBindMediaAssetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.bindMaterialCover(request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/dialogue-lines")
    public ApiResponse<PageResult<AdminDialogueLineVO>> pageLines(@Valid AdminDialogueLineQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.pageLines(query, admin));
    }

    @PostMapping("/dialogue-lines")
    public ApiResponse<AdminDialogueLineVO> createLine(
            @Valid @RequestBody AdminUpsertDialogueLineDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.createLine(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/dialogue-lines/{lineId}")
    public ApiResponse<AdminDialogueLineVO> updateLine(
            @PathVariable Long lineId,
            @Valid @RequestBody AdminUpsertDialogueLineDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.updateLine(lineId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/dialogue-lines/audio-bindings")
    public ApiResponse<AdminBatchBindMediaAssetResultVO> bindDialogueLineAudio(
            @Valid @RequestBody AdminBatchBindMediaAssetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.bindDialogueLineAudio(request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/dialogue-line-vocab")
    public ApiResponse<PageResult<AdminDialogueLineVocabVO>> pageLineVocab(@Valid AdminDialogueLineVocabQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.pageLineVocab(query, admin));
    }

    @PostMapping("/dialogue-line-vocab")
    public ApiResponse<AdminDialogueLineVocabVO> createLineVocab(
            @Valid @RequestBody AdminUpsertDialogueLineVocabDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.createLineVocab(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/dialogue-line-vocab/{vocabId}")
    public ApiResponse<AdminDialogueLineVocabVO> updateLineVocab(
            @PathVariable Long vocabId,
            @Valid @RequestBody AdminUpsertDialogueLineVocabDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDialogueManagementService.updateLineVocab(vocabId, request, admin, clientIp(servletRequest)));
    }

    @DeleteMapping("/dialogue-line-vocab/{vocabId}")
    public ApiResponse<Void> deleteLineVocab(
            @PathVariable Long vocabId,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        adminDialogueManagementService.deleteLineVocab(vocabId, admin, clientIp(servletRequest));
        return ApiResponse.ok(null);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
