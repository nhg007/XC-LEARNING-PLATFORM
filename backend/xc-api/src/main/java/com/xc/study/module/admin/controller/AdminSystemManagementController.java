package com.xc.study.module.admin.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAccountQueryDTO;
import com.xc.study.module.admin.dto.AdminCreateAccountDTO;
import com.xc.study.module.admin.dto.AdminOperationLogQueryDTO;
import com.xc.study.module.admin.dto.AdminResetAccountPasswordDTO;
import com.xc.study.module.admin.dto.AdminSystemConfigQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateAccountDTO;
import com.xc.study.module.admin.dto.AdminUpdateAccountRolesDTO;
import com.xc.study.module.admin.dto.AdminUpdateMatchingStagesDTO;
import com.xc.study.module.admin.dto.AdminUpdateRolePermissionsDTO;
import com.xc.study.module.admin.dto.AdminUpdateRuntimeSettingsDTO;
import com.xc.study.module.admin.dto.AdminUpdateSystemConfigDTO;
import com.xc.study.module.admin.dto.AdminUpsertRoleDTO;
import com.xc.study.module.admin.service.AdminSystemManagementService;
import com.xc.study.module.admin.vo.AdminAccountVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPermissionVO;
import com.xc.study.module.admin.vo.AdminRuntimeSettingsVO;
import com.xc.study.module.admin.vo.AdminRoleVO;
import com.xc.study.module.admin.vo.AdminSystemConfigVO;
import com.xc.study.module.matching.vo.MatchingStageVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
public class AdminSystemManagementController {

    private final AdminSystemManagementService adminSystemManagementService;
    private final CurrentUserProvider currentUserProvider;

    public AdminSystemManagementController(
            AdminSystemManagementService adminSystemManagementService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminSystemManagementService = adminSystemManagementService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/roles")
    public ApiResponse<List<AdminRoleVO>> listRoles() {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.listRoles(admin));
    }

    @GetMapping("/admin-users")
    public ApiResponse<PageResult<AdminAccountVO>> pageAdminUsers(@Valid AdminAccountQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.pageAdminUsers(query, admin));
    }

    @PostMapping("/admin-users")
    public ApiResponse<AdminAccountVO> createAdminUser(
            @Valid @RequestBody AdminCreateAccountDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.createAdminUser(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/admin-users/{adminUserId}")
    public ApiResponse<AdminAccountVO> updateAdminUser(
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminUpdateAccountDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateAdminUser(adminUserId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/admin-users/{adminUserId}/roles")
    public ApiResponse<AdminAccountVO> updateAdminUserRoles(
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminUpdateAccountRolesDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateAdminUserRoles(adminUserId, request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/admin-users/{adminUserId}/password")
    public ApiResponse<AdminAccountVO> resetAdminUserPassword(
            @PathVariable Long adminUserId,
            @Valid @RequestBody AdminResetAccountPasswordDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.resetAdminUserPassword(adminUserId, request, admin, clientIp(servletRequest)));
    }

    @PostMapping("/roles")
    public ApiResponse<AdminRoleVO> createRole(
            @Valid @RequestBody AdminUpsertRoleDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.createRole(request, admin, clientIp(servletRequest)));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<AdminRoleVO> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody AdminUpsertRoleDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateRole(roleId, request, admin, clientIp(servletRequest)));
    }

    @DeleteMapping("/roles/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId, HttpServletRequest servletRequest) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        adminSystemManagementService.deleteRole(roleId, admin, clientIp(servletRequest));
        return ApiResponse.ok(null);
    }

    @PostMapping("/roles/{roleId}/delete")
    public ApiResponse<Void> deleteRoleByPost(@PathVariable Long roleId, HttpServletRequest servletRequest) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        adminSystemManagementService.deleteRole(roleId, admin, clientIp(servletRequest));
        return ApiResponse.ok(null);
    }

    @PutMapping("/roles/{roleId}/permissions")
    public ApiResponse<AdminRoleVO> updateRolePermissions(
            @PathVariable Long roleId,
            @Valid @RequestBody AdminUpdateRolePermissionsDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateRolePermissions(
                roleId,
                request,
                admin,
                clientIp(servletRequest)
        ));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<AdminPermissionVO>> listPermissions() {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.listPermissions(admin));
    }

    @GetMapping("/system-configs")
    public ApiResponse<PageResult<AdminSystemConfigVO>> pageSystemConfigs(@Valid AdminSystemConfigQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.pageSystemConfigs(query, admin));
    }

    @GetMapping("/system-configs/matching-stages")
    public ApiResponse<List<MatchingStageVO>> listMatchingStages() {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.listMatchingStages(admin));
    }

    @GetMapping("/system-configs/runtime-settings")
    public ApiResponse<AdminRuntimeSettingsVO> getRuntimeSettings() {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.getRuntimeSettings(admin));
    }

    @PutMapping("/system-configs/runtime-settings")
    public ApiResponse<AdminRuntimeSettingsVO> updateRuntimeSettings(
            @Valid @RequestBody AdminUpdateRuntimeSettingsDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateRuntimeSettings(
                request,
                admin,
                clientIp(servletRequest)
        ));
    }

    @PutMapping("/system-configs/matching-stages")
    public ApiResponse<List<MatchingStageVO>> updateMatchingStages(
            @Valid @RequestBody AdminUpdateMatchingStagesDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateMatchingStages(
                request,
                admin,
                clientIp(servletRequest)
        ));
    }

    @PutMapping("/system-configs/{configKey}")
    public ApiResponse<AdminSystemConfigVO> updateSystemConfig(
            @PathVariable String configKey,
            @Valid @RequestBody AdminUpdateSystemConfigDTO request,
            HttpServletRequest servletRequest
    ) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.updateSystemConfig(
                configKey,
                request,
                admin,
                clientIp(servletRequest)
        ));
    }

    @GetMapping("/operation-logs")
    public ApiResponse<PageResult<AdminOperationLogVO>> pageOperationLogs(@Valid AdminOperationLogQueryDTO query) {
        CurrentUser admin = currentUserProvider.requireAdmin();
        return ApiResponse.ok(adminSystemManagementService.pageOperationLogs(query, admin));
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
