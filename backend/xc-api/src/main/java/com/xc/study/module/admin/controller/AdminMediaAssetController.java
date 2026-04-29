package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminMediaAssetQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUploadMediaAssetDTO;
import com.xc.study.module.admin.service.AdminMediaAssetService;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminMediaAssetVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/media-assets")
public class AdminMediaAssetController {

    private final AdminMediaAssetService adminMediaAssetService;
    private final CurrentUserProvider currentUserProvider;

    public AdminMediaAssetController(AdminMediaAssetService adminMediaAssetService, CurrentUserProvider currentUserProvider) {
        this.adminMediaAssetService = adminMediaAssetService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminMediaAssetVO>> pageAssets(@Valid AdminMediaAssetQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMediaAssetService.pageAssets(query, admin));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminMediaAssetVO> upload(
            @Valid @ModelAttribute AdminUploadMediaAssetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMediaAssetService.upload(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/{assetId}/status")
    public ApiResponse<AdminMediaAssetVO> updateStatus(
            @PathVariable Long assetId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMediaAssetService.updateStatus(assetId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/status/batch")
    public ApiResponse<AdminBatchContentStatusResultVO> updateStatuses(
            @Valid @RequestBody AdminBatchUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminMediaAssetService.updateStatuses(request, admin, clientIp(servletRequest)));
    }

    @DeleteMapping("/{assetId}")
    public ApiResponse<Void> delete(
            @PathVariable Long assetId,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        adminMediaAssetService.delete(assetId, admin, clientIp(servletRequest));
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
