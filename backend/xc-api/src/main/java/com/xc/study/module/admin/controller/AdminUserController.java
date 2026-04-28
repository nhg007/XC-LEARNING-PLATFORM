package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAdjustMembershipDTO;
import com.xc.study.module.admin.dto.AdminUpdateUserStatusDTO;
import com.xc.study.module.admin.dto.AdminUserQueryDTO;
import com.xc.study.module.admin.service.AdminUserService;
import com.xc.study.module.admin.vo.AdminUserDetailVO;
import com.xc.study.module.admin.vo.AdminUserListItemVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CurrentUserProvider currentUserProvider;

    public AdminUserController(AdminUserService adminUserService, CurrentUserProvider currentUserProvider) {
        this.adminUserService = adminUserService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserListItemVO>> pageUsers(@Valid AdminUserQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminUserService.pageUsers(query, admin));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserDetailVO> getUserDetail(@PathVariable Long userId) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminUserService.getUserDetail(userId, admin));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<AdminUserDetailVO> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserStatusDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminUserService.updateUserStatus(userId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/{userId}/membership")
    public ApiResponse<AdminUserDetailVO> adjustMembership(
            @PathVariable Long userId,
            @Valid @RequestBody AdminAdjustMembershipDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminUserService.adjustMembership(userId, request, admin, clientIp(servletRequest)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
