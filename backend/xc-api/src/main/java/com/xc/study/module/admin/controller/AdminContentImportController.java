package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.admin.service.AdminContentImportService;
import com.xc.study.module.admin.vo.AdminContentImportResultVO;
import com.xc.study.module.admin.vo.AdminContentImportTemplateVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/admin/content-import")
public class AdminContentImportController {

    private final AdminContentImportService adminContentImportService;
    private final CurrentUserProvider currentUserProvider;

    public AdminContentImportController(
            AdminContentImportService adminContentImportService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminContentImportService = adminContentImportService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/templates/{importType}")
    public ApiResponse<AdminContentImportTemplateVO> template(@PathVariable String importType) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminContentImportService.template(importType, admin));
    }

    @PostMapping(value = "/{importType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminContentImportResultVO> importCsv(
            @PathVariable String importType,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminContentImportService.importCsv(importType, file, admin, clientIp(servletRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
