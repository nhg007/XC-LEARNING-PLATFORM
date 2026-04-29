package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabItemDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabListDTO;
import com.xc.study.module.admin.dto.AdminVocabItemQueryDTO;
import com.xc.study.module.admin.dto.AdminVocabListQueryDTO;
import com.xc.study.module.admin.service.AdminVocabManagementService;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminVocabItemVO;
import com.xc.study.module.admin.vo.AdminVocabListVO;
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
public class AdminVocabManagementController {

    private final AdminVocabManagementService adminVocabManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminVocabManagementController(
            AdminVocabManagementService adminVocabManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminVocabManagementService = adminVocabManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/vocab-lists")
    public ApiResponse<PageResult<AdminVocabListVO>> pageLists(@Valid AdminVocabListQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.pageLists(query, admin));
    }

    @PostMapping("/vocab-lists")
    public ApiResponse<AdminVocabListVO> createList(
            @Valid @RequestBody AdminUpsertVocabListDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.createList(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-lists/{listId}")
    public ApiResponse<AdminVocabListVO> updateList(
            @PathVariable Long listId,
            @Valid @RequestBody AdminUpsertVocabListDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateList(listId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-lists/{listId}/status")
    public ApiResponse<AdminVocabListVO> updateListStatus(
            @PathVariable Long listId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateListStatus(listId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-lists/status/batch")
    public ApiResponse<AdminBatchContentStatusResultVO> updateListStatuses(
            @Valid @RequestBody AdminBatchUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateListStatuses(request, admin, clientIp(servletRequest)));
    }

    @GetMapping("/vocab-items")
    public ApiResponse<PageResult<AdminVocabItemVO>> pageItems(@Valid AdminVocabItemQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.pageItems(query, admin));
    }

    @PostMapping("/vocab-items")
    public ApiResponse<AdminVocabItemVO> createItem(
            @Valid @RequestBody AdminUpsertVocabItemDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.createItem(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-items/{itemId}")
    public ApiResponse<AdminVocabItemVO> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminUpsertVocabItemDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateItem(itemId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-items/{itemId}/status")
    public ApiResponse<AdminVocabItemVO> updateItemStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody AdminUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateItemStatus(itemId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-items/status/batch")
    public ApiResponse<AdminBatchContentStatusResultVO> updateItemStatuses(
            @Valid @RequestBody AdminBatchUpdateContentStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.updateItemStatuses(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/vocab-items/audio-bindings")
    public ApiResponse<AdminBatchBindMediaAssetResultVO> bindItemAudio(
            @Valid @RequestBody AdminBatchBindMediaAssetDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminVocabManagementService.bindItemAudio(request, admin, clientIp(servletRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
