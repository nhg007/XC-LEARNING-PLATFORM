package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminLeaderboardQueryDTO;
import com.xc.study.module.admin.dto.AdminLearningReportQueryDTO;
import com.xc.study.module.admin.service.AdminReportService;
import com.xc.study.module.admin.vo.AdminLeaderboardEntryVO;
import com.xc.study.module.admin.vo.AdminLearningReportVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin")
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final CurrentUserProvider currentUserProvider;

    public AdminReportController(
            AdminReportService adminReportService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminReportService = adminReportService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/reports/learning")
    public ApiResponse<AdminLearningReportVO> learningReport(@Valid AdminLearningReportQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminReportService.learningReport(query, admin));
    }

    @GetMapping("/leaderboards")
    public ApiResponse<PageResult<AdminLeaderboardEntryVO>> pageLeaderboards(@Valid AdminLeaderboardQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminReportService.pageLeaderboards(query, admin));
    }
}
