package com.xc.study.module.admin.service;

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
import com.xc.study.module.admin.vo.AdminAccountVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPermissionVO;
import com.xc.study.module.admin.vo.AdminRuntimeSettingsVO;
import com.xc.study.module.admin.vo.AdminRoleVO;
import com.xc.study.module.admin.vo.AdminSystemConfigVO;
import com.xc.study.module.matching.vo.MatchingStageVO;
import com.xc.study.security.CurrentUser;
import java.util.List;

public interface AdminSystemManagementService {

    List<AdminRoleVO> listRoles(CurrentUser admin);

    List<AdminPermissionVO> listPermissions(CurrentUser admin);

    PageResult<AdminAccountVO> pageAdminUsers(AdminAccountQueryDTO query, CurrentUser admin);

    AdminAccountVO createAdminUser(AdminCreateAccountDTO request, CurrentUser admin, String ipAddress);

    AdminAccountVO updateAdminUser(Long adminUserId, AdminUpdateAccountDTO request, CurrentUser admin, String ipAddress);

    AdminAccountVO updateAdminUserRoles(Long adminUserId, AdminUpdateAccountRolesDTO request, CurrentUser admin, String ipAddress);

    AdminAccountVO resetAdminUserPassword(Long adminUserId, AdminResetAccountPasswordDTO request, CurrentUser admin, String ipAddress);

    AdminRoleVO createRole(AdminUpsertRoleDTO request, CurrentUser admin, String ipAddress);

    AdminRoleVO updateRole(Long roleId, AdminUpsertRoleDTO request, CurrentUser admin, String ipAddress);

    void deleteRole(Long roleId, CurrentUser admin, String ipAddress);

    AdminRoleVO updateRolePermissions(Long roleId, AdminUpdateRolePermissionsDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminSystemConfigVO> pageSystemConfigs(AdminSystemConfigQueryDTO query, CurrentUser admin);

    AdminSystemConfigVO updateSystemConfig(String configKey, AdminUpdateSystemConfigDTO request, CurrentUser admin, String ipAddress);

    List<MatchingStageVO> listMatchingStages(CurrentUser admin);

    List<MatchingStageVO> updateMatchingStages(AdminUpdateMatchingStagesDTO request, CurrentUser admin, String ipAddress);

    AdminRuntimeSettingsVO getRuntimeSettings(CurrentUser admin);

    AdminRuntimeSettingsVO updateRuntimeSettings(AdminUpdateRuntimeSettingsDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminOperationLogVO> pageOperationLogs(AdminOperationLogQueryDTO query, CurrentUser admin);
}
