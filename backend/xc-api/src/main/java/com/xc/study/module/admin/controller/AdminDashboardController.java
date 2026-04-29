package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.admin.service.AdminDashboardService;
import com.xc.study.module.admin.vo.AdminDashboardSummaryVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final CurrentUserProvider currentUserProvider;

    public AdminDashboardController(AdminDashboardService adminDashboardService, CurrentUserProvider currentUserProvider) {
        this.adminDashboardService = adminDashboardService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryVO> summary() {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminDashboardService.summary(admin));
    }
}
