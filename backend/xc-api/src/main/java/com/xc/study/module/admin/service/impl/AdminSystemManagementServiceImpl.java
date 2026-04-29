package com.xc.study.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminAccountQueryDTO;
import com.xc.study.module.admin.dto.AdminCreateAccountDTO;
import com.xc.study.module.admin.dto.AdminOperationLogQueryDTO;
import com.xc.study.module.admin.dto.AdminResetAccountPasswordDTO;
import com.xc.study.module.admin.dto.AdminSystemConfigQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateAccountDTO;
import com.xc.study.module.admin.dto.AdminUpdateAccountRolesDTO;
import com.xc.study.module.admin.dto.AdminUpdateRolePermissionsDTO;
import com.xc.study.module.admin.dto.AdminUpdateSystemConfigDTO;
import com.xc.study.module.admin.dto.AdminUpsertRoleDTO;
import com.xc.study.module.admin.entity.AdminOperationLog;
import com.xc.study.module.admin.entity.AdminPermission;
import com.xc.study.module.admin.entity.AdminRole;
import com.xc.study.module.admin.entity.AdminRolePermission;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.entity.AdminUserRole;
import com.xc.study.module.admin.entity.SystemConfig;
import com.xc.study.module.admin.mapper.AdminOperationLogMapper;
import com.xc.study.module.admin.mapper.AdminPermissionMapper;
import com.xc.study.module.admin.mapper.AdminRoleMapper;
import com.xc.study.module.admin.mapper.AdminRolePermissionMapper;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.admin.mapper.AdminUserRoleMapper;
import com.xc.study.module.admin.mapper.SystemConfigMapper;
import com.xc.study.module.admin.service.AdminSystemManagementService;
import com.xc.study.module.admin.service.support.AdminSorts;
import com.xc.study.module.admin.vo.AdminAccountVO;
import com.xc.study.module.admin.vo.AdminOperationLogVO;
import com.xc.study.module.admin.vo.AdminPermissionVO;
import com.xc.study.module.admin.vo.AdminRoleVO;
import com.xc.study.module.admin.vo.AdminSystemConfigVO;
import com.xc.study.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminSystemManagementServiceImpl implements AdminSystemManagementService {

    private final AdminRoleMapper adminRoleMapper;
    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final SystemConfigMapper systemConfigMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminSystemManagementServiceImpl(
            AdminRoleMapper adminRoleMapper,
            AdminUserMapper adminUserMapper,
            AdminUserRoleMapper adminUserRoleMapper,
            AdminPermissionMapper adminPermissionMapper,
            AdminRolePermissionMapper adminRolePermissionMapper,
            SystemConfigMapper systemConfigMapper,
            AdminOperationLogMapper adminOperationLogMapper,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.adminRoleMapper = adminRoleMapper;
        this.adminUserMapper = adminUserMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.adminPermissionMapper = adminPermissionMapper;
        this.adminRolePermissionMapper = adminRolePermissionMapper;
        this.systemConfigMapper = systemConfigMapper;
        this.adminOperationLogMapper = adminOperationLogMapper;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<AdminRoleVO> listRoles(CurrentUser admin) {
        requirePermission(admin, "admin:system:read");
        List<AdminRole> roles = adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>()
                .orderByAsc(AdminRole::getRoleCode)
                .orderByAsc(AdminRole::getId));
        Map<Long, List<AdminPermissionVO>> permissionsByRole = loadPermissionsByRoleIds(
                roles.stream().map(AdminRole::getId).toList());
        return roles.stream()
                .map(role -> toRoleVO(role, permissionsByRole.getOrDefault(role.getId(), List.of())))
                .toList();
    }

    @Override
    public List<AdminPermissionVO> listPermissions(CurrentUser admin) {
        requirePermission(admin, "admin:system:read");
        return adminPermissionMapper.selectList(new LambdaQueryWrapper<AdminPermission>()
                        .orderByAsc(AdminPermission::getModuleName)
                        .orderByAsc(AdminPermission::getPermissionCode)
                        .orderByAsc(AdminPermission::getId))
                .stream()
                .map(this::toPermissionVO)
                .toList();
    }

    @Override
    public PageResult<AdminAccountVO> pageAdminUsers(AdminAccountQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:system:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(AdminUser::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(AdminUser::getUsername, keyword)
                    .or()
                    .like(AdminUser::getDisplayName, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", AdminUser::getId,
                "username", AdminUser::getUsername,
                "displayName", AdminUser::getDisplayName,
                "status", AdminUser::getStatus,
                "lastLoginAt", AdminUser::getLastLoginAt,
                "createdAt", AdminUser::getCreatedAt,
                "updatedAt", AdminUser::getUpdatedAt
        ));
        if (!sorted) {
            wrapper.orderByDesc(AdminUser::getCreatedAt);
        }
        wrapper.orderByDesc(AdminUser::getId);
        Page<AdminUser> result = adminUserMapper.selectPage(Page.of(page, pageSize), wrapper);
        Map<Long, List<AdminRoleVO>> rolesByUser = loadRolesByAdminUserIds(result.getRecords().stream()
                .map(AdminUser::getId)
                .toList());
        List<AdminAccountVO> records = result.getRecords()
                .stream()
                .map(user -> toAdminAccountVO(user, rolesByUser.getOrDefault(user.getId(), List.of())))
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminAccountVO createAdminUser(AdminCreateAccountDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:system:update");
        String username = normalizeUsername(request.username());
        ensureUsernameAvailable(username, null);
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        OffsetDateTime now = OffsetDateTime.now();
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(displayNameOrUsername(request.displayName(), username));
        user.setStatus(request.status());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        adminUserMapper.insert(user);
        replaceAdminUserRoles(user.getId(), roleIds, now);
        writeOperationLog(
                admin.id(),
                "admin_user_create",
                "admin_user",
                user.getId(),
                Map.of(
                        "username", user.getUsername(),
                        "displayName", user.getDisplayName(),
                        "status", user.getStatus(),
                        "roleIds", roleIds
                ),
                ipAddress
        );
        return buildAdminAccountVO(user.getId());
    }

    @Override
    @Transactional
    public AdminAccountVO updateAdminUser(
            Long adminUserId,
            AdminUpdateAccountDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:system:update");
        AdminUser user = requireAdminUser(adminUserId);
        if (adminUserId.equals(admin.id()) && "disabled".equals(request.status())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能禁用当前登录管理员");
        }
        Map<String, Object> before = adminUserSnapshot(user);
        user.setDisplayName(displayNameOrUsername(request.displayName(), user.getUsername()));
        user.setStatus(request.status());
        user.setUpdatedAt(OffsetDateTime.now());
        adminUserMapper.updateById(user);
        writeOperationLog(
                admin.id(),
                "admin_user_update",
                "admin_user",
                user.getId(),
                Map.of("before", before, "after", adminUserSnapshot(user)),
                ipAddress
        );
        return buildAdminAccountVO(user.getId());
    }

    @Override
    @Transactional
    public AdminAccountVO updateAdminUserRoles(
            Long adminUserId,
            AdminUpdateAccountRolesDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:system:update");
        if (adminUserId.equals(admin.id())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能调整当前登录管理员的角色");
        }
        AdminUser user = requireAdminUser(adminUserId);
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        List<Long> oldRoleIds = loadAdminUserRoleIds(adminUserId);
        replaceAdminUserRoles(adminUserId, roleIds, OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        adminUserMapper.updateById(user);
        writeOperationLog(
                admin.id(),
                "admin_user_roles_update",
                "admin_user",
                adminUserId,
                Map.of(
                        "username", user.getUsername(),
                        "oldRoleIds", oldRoleIds,
                        "newRoleIds", roleIds
                ),
                ipAddress
        );
        return buildAdminAccountVO(adminUserId);
    }

    @Override
    @Transactional
    public AdminAccountVO resetAdminUserPassword(
            Long adminUserId,
            AdminResetAccountPasswordDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:system:update");
        AdminUser user = requireAdminUser(adminUserId);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUpdatedAt(OffsetDateTime.now());
        adminUserMapper.updateById(user);
        writeOperationLog(
                admin.id(),
                "admin_user_password_reset",
                "admin_user",
                adminUserId,
                Map.of("username", user.getUsername(), "passwordChanged", true),
                ipAddress
        );
        return buildAdminAccountVO(adminUserId);
    }

    @Override
    @Transactional
    public AdminRoleVO createRole(AdminUpsertRoleDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:system:update");
        String roleCode = normalizeRoleCode(request.roleCode());
        ensureRoleCodeAvailable(roleCode, null);
        OffsetDateTime now = OffsetDateTime.now();
        AdminRole role = new AdminRole();
        role.setRoleCode(roleCode);
        role.setRoleName(request.roleName().trim());
        role.setDescription(blankToNull(request.description()));
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        adminRoleMapper.insert(role);
        writeOperationLog(
                admin.id(),
                "admin_role_create",
                "admin_role",
                role.getId(),
                roleSnapshot(role),
                ipAddress
        );
        return toRoleVO(role, List.of());
    }

    @Override
    @Transactional
    public AdminRoleVO updateRole(Long roleId, AdminUpsertRoleDTO request, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:system:update");
        AdminRole role = requireRole(roleId);
        Map<String, Object> before = roleSnapshot(role);
        String roleCode = normalizeRoleCode(request.roleCode());
        if ("super_admin".equals(role.getRoleCode()) && !role.getRoleCode().equals(roleCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "超级管理员角色编码不能修改");
        }
        ensureRoleCodeAvailable(roleCode, roleId);
        role.setRoleCode(roleCode);
        role.setRoleName(request.roleName().trim());
        role.setDescription(blankToNull(request.description()));
        role.setUpdatedAt(OffsetDateTime.now());
        adminRoleMapper.updateById(role);
        writeOperationLog(
                admin.id(),
                "admin_role_update",
                "admin_role",
                role.getId(),
                Map.of("before", before, "after", roleSnapshot(role)),
                ipAddress
        );
        return toRoleVO(role, loadPermissionsByRoleIds(List.of(roleId)).getOrDefault(roleId, List.of()));
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId, CurrentUser admin, String ipAddress) {
        requirePermission(admin, "admin:system:update");
        AdminRole role = requireRole(roleId);
        if ("super_admin".equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "超级管理员角色不能删除");
        }
        Long assignedCount = adminUserRoleMapper.selectCount(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getRoleId, roleId));
        if (assignedCount != null && assignedCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该角色已分配给管理员，请先移除绑定后再删除");
        }
        Map<String, Object> snapshot = roleSnapshot(role);
        adminRolePermissionMapper.delete(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId));
        adminRoleMapper.deleteById(roleId);
        writeOperationLog(
                admin.id(),
                "admin_role_delete",
                "admin_role",
                roleId,
                snapshot,
                ipAddress
        );
    }

    @Override
    @Transactional
    public AdminRoleVO updateRolePermissions(
            Long roleId,
            AdminUpdateRolePermissionsDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:system:update");
        AdminRole role = requireRole(roleId);
        List<Long> permissionIds = normalizePermissionIds(request.getPermissionIds());
        Map<Long, AdminPermission> permissions = loadPermissionMap(permissionIds);
        List<Long> oldPermissionIds = adminRolePermissionMapper.selectList(new LambdaQueryWrapper<AdminRolePermission>()
                        .eq(AdminRolePermission::getRoleId, roleId)
                        .orderByAsc(AdminRolePermission::getPermissionId))
                .stream()
                .map(AdminRolePermission::getPermissionId)
                .toList();

        adminRolePermissionMapper.delete(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId));
        OffsetDateTime now = OffsetDateTime.now();
        for (Long permissionId : permissionIds) {
            AdminRolePermission relation = new AdminRolePermission();
            relation.setRoleId(roleId);
            relation.setPermissionId(permissionId);
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            adminRolePermissionMapper.insert(relation);
        }
        role.setUpdatedAt(now);
        adminRoleMapper.updateById(role);
        writeOperationLog(
                admin.id(),
                "admin_role_permissions_update",
                "admin_role",
                roleId,
                Map.of(
                        "roleCode", role.getRoleCode(),
                        "oldPermissionIds", oldPermissionIds,
                        "newPermissionIds", permissionIds
                ),
                ipAddress
        );
        List<AdminPermissionVO> permissionVOs = permissionIds.stream()
                .map(permissions::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(AdminPermission::getModuleName)
                        .thenComparing(AdminPermission::getPermissionCode)
                        .thenComparing(AdminPermission::getId))
                .map(this::toPermissionVO)
                .toList();
        return toRoleVO(role, permissionVOs);
    }

    @Override
    public PageResult<AdminSystemConfigVO> pageSystemConfigs(AdminSystemConfigQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:system:read");
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getConfigGroup())) {
            wrapper.eq(SystemConfig::getConfigGroup, query.getConfigGroup());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(inner -> inner.like(SystemConfig::getConfigKey, keyword)
                    .or()
                    .like(SystemConfig::getDescription, keyword));
        }
        boolean sorted = AdminSorts.apply(wrapper, query.getSortBy(), query.getSortDirection(), Map.of(
                "id", SystemConfig::getId,
                "configGroup", SystemConfig::getConfigGroup,
                "configKey", SystemConfig::getConfigKey,
                "updatedAt", SystemConfig::getUpdatedAt,
                "updatedBy", SystemConfig::getUpdatedBy
        ));
        if (!sorted) {
            wrapper.orderByAsc(SystemConfig::getConfigGroup)
                    .orderByAsc(SystemConfig::getConfigKey);
        }
        wrapper.orderByAsc(SystemConfig::getId);
        Page<SystemConfig> result = systemConfigMapper.selectPage(Page.of(page, pageSize), wrapper);
        List<AdminSystemConfigVO> records = result.getRecords().stream()
                .map(this::toSystemConfigVO)
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional
    public AdminSystemConfigVO updateSystemConfig(
            String configKey,
            AdminUpdateSystemConfigDTO request,
            CurrentUser admin,
            String ipAddress
    ) {
        requirePermission(admin, "admin:system:update");
        SystemConfig config = requireSystemConfig(configKey);
        String oldDescription = config.getDescription();
        config.setConfigValue(request.getConfigValue());
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        config.setUpdatedBy(admin.id());
        config.setUpdatedAt(OffsetDateTime.now());
        systemConfigMapper.updateById(config);
        writeOperationLog(
                admin.id(),
                "system_config_update",
                "system_config",
                config.getId(),
                Map.of(
                        "configKey", config.getConfigKey(),
                        "configGroup", config.getConfigGroup(),
                        "valueChanged", true,
                        "descriptionChanged", !Objects.equals(oldDescription, config.getDescription())
                ),
                ipAddress
        );
        return toSystemConfigVO(config);
    }

    @Override
    public PageResult<AdminOperationLogVO> pageOperationLogs(AdminOperationLogQueryDTO query, CurrentUser admin) {
        requirePermission(admin, "admin:audit:read");
        if (query.getCreatedFrom() != null && query.getCreatedTo() != null
                && query.getCreatedFrom().isAfter(query.getCreatedTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "开始时间不能晚于结束时间");
        }
        int page = query.getPage() == null ? 1 : query.getPage();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        Page<AdminOperationLog> result = adminOperationLogMapper.selectLogPage(Page.of(page, pageSize), query);
        List<AdminOperationLogVO> records = result.getRecords().stream()
                .map(this::toOperationLogVO)
                .toList();
        return PageResult.of(records, result.getTotal(), result.getCurrent(), result.getSize());
    }

    private Map<Long, List<AdminPermissionVO>> loadPermissionsByRoleIds(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AdminRolePermission> relations = adminRolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                        .in(AdminRolePermission::getRoleId, roleIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> permissionIds = relations.stream()
                .map(AdminRolePermission::getPermissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AdminPermission> permissions = loadPermissionMap(permissionIds);
        Map<Long, List<AdminPermissionVO>> result = new HashMap<>();
        relations.forEach(relation -> {
            AdminPermission permission = permissions.get(relation.getPermissionId());
            if (permission == null) {
                return;
            }
            result.computeIfAbsent(relation.getRoleId(), ignored -> new ArrayList<>())
                    .add(toPermissionVO(permission));
        });
        result.replaceAll((roleId, permissionsForRole) -> permissionsForRole.stream()
                .sorted(Comparator.comparing(AdminPermissionVO::moduleName)
                        .thenComparing(AdminPermissionVO::permissionCode)
                        .thenComparing(AdminPermissionVO::id))
                .toList());
        return result;
    }

    private Map<Long, List<AdminRoleVO>> loadRolesByAdminUserIds(List<Long> adminUserIds) {
        if (adminUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<AdminUserRole> relations = adminUserRoleMapper.selectList(new LambdaQueryWrapper<AdminUserRole>()
                .in(AdminUserRole::getAdminUserId, adminUserIds)
                .orderByAsc(AdminUserRole::getAdminUserId)
                .orderByAsc(AdminUserRole::getRoleId));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> roleIds = relations.stream()
                .map(AdminUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AdminRole> roles = loadRoleMap(roleIds);
        Map<Long, List<AdminPermissionVO>> permissionsByRole = loadPermissionsByRoleIds(roleIds);
        Map<Long, List<AdminRoleVO>> result = new LinkedHashMap<>();
        relations.forEach(relation -> {
            AdminRole role = roles.get(relation.getRoleId());
            if (role == null) {
                return;
            }
            result.computeIfAbsent(relation.getAdminUserId(), ignored -> new ArrayList<>())
                    .add(toRoleVO(role, permissionsByRole.getOrDefault(role.getId(), List.of())));
        });
        result.replaceAll((userId, userRoles) -> userRoles.stream()
                .sorted(Comparator.comparing(AdminRoleVO::roleCode).thenComparing(AdminRoleVO::id))
                .toList());
        return result;
    }

    private Map<Long, AdminRole> loadRoleMap(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, AdminRole> roles = adminRoleMapper.selectBatchIds(roleIds)
                .stream()
                .collect(Collectors.toMap(AdminRole::getId, Function.identity()));
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "角色不存在");
        }
        return roles;
    }

    private List<Long> normalizeRoleIds(List<Long> rawIds) {
        Set<Long> roleIds = new LinkedHashSet<>();
        for (Long roleId : rawIds) {
            if (roleId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "角色 ID 不能为空");
            }
            roleIds.add(roleId);
        }
        List<Long> normalizedIds = new ArrayList<>(roleIds);
        loadRoleMap(normalizedIds);
        return normalizedIds;
    }

    private List<Long> loadAdminUserRoleIds(Long adminUserId) {
        return adminUserRoleMapper.selectList(new LambdaQueryWrapper<AdminUserRole>()
                        .eq(AdminUserRole::getAdminUserId, adminUserId)
                        .orderByAsc(AdminUserRole::getRoleId))
                .stream()
                .map(AdminUserRole::getRoleId)
                .toList();
    }

    private void replaceAdminUserRoles(Long adminUserId, List<Long> roleIds, OffsetDateTime now) {
        adminUserRoleMapper.delete(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminUserId));
        for (Long roleId : roleIds) {
            AdminUserRole relation = new AdminUserRole();
            relation.setAdminUserId(adminUserId);
            relation.setRoleId(roleId);
            relation.setCreatedAt(now);
            relation.setUpdatedAt(now);
            adminUserRoleMapper.insert(relation);
        }
    }

    private Map<Long, AdminPermission> loadPermissionMap(List<Long> permissionIds) {
        if (permissionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, AdminPermission> permissions = adminPermissionMapper.selectBatchIds(permissionIds)
                .stream()
                .collect(Collectors.toMap(AdminPermission::getId, Function.identity()));
        if (permissions.size() != permissionIds.size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "权限不存在");
        }
        return permissions;
    }

    private List<Long> normalizePermissionIds(List<Long> rawIds) {
        Set<Long> permissionIds = new LinkedHashSet<>();
        for (Long permissionId : rawIds) {
            if (permissionId == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "权限 ID 不能为空");
            }
            permissionIds.add(permissionId);
        }
        return new ArrayList<>(permissionIds);
    }

    private AdminRole requireRole(Long roleId) {
        AdminRole role = adminRoleMapper.selectById(roleId);
        if (role == null) {
            throw BusinessException.notFound("角色不存在");
        }
        return role;
    }

    private AdminUser requireAdminUser(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null) {
            throw BusinessException.notFound("管理员不存在");
        }
        return adminUser;
    }

    private void ensureUsernameAvailable(String username, Long currentAdminUserId) {
        AdminUser existing = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), currentAdminUserId)) {
            throw BusinessException.conflict("管理员账号已存在");
        }
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "管理员账号不能为空");
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String displayNameOrUsername(String displayName, String username) {
        return StringUtils.hasText(displayName) ? displayName.trim() : username;
    }

    private void ensureRoleCodeAvailable(String roleCode, Long currentRoleId) {
        AdminRole existing = adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getRoleCode, roleCode)
                .last("limit 1"));
        if (existing != null && !Objects.equals(existing.getId(), currentRoleId)) {
            throw BusinessException.conflict("角色编码已存在");
        }
    }

    private String normalizeRoleCode(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "角色编码不能为空");
        }
        return roleCode.trim();
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SystemConfig requireSystemConfig(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "配置键不能为空");
        }
        SystemConfig config = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey.trim())
                .last("limit 1"));
        if (config == null) {
            throw BusinessException.notFound("系统配置不存在");
        }
        return config;
    }

    private AdminRoleVO toRoleVO(AdminRole role, List<AdminPermissionVO> permissions) {
        return new AdminRoleVO(
                role.getId(),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                permissions,
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private AdminAccountVO buildAdminAccountVO(Long adminUserId) {
        AdminUser user = requireAdminUser(adminUserId);
        List<AdminRoleVO> roles = loadRolesByAdminUserIds(List.of(adminUserId)).getOrDefault(adminUserId, List.of());
        return toAdminAccountVO(user, roles);
    }

    private AdminAccountVO toAdminAccountVO(AdminUser user, List<AdminRoleVO> roles) {
        return new AdminAccountVO(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getStatus(),
                user.getLastLoginAt(),
                roles,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Map<String, Object> roleSnapshot(AdminRole role) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("roleCode", role.getRoleCode());
        snapshot.put("roleName", role.getRoleName());
        snapshot.put("description", role.getDescription());
        return snapshot;
    }

    private Map<String, Object> adminUserSnapshot(AdminUser user) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("username", user.getUsername());
        snapshot.put("displayName", user.getDisplayName());
        snapshot.put("status", user.getStatus());
        return snapshot;
    }

    private AdminPermissionVO toPermissionVO(AdminPermission permission) {
        return new AdminPermissionVO(
                permission.getId(),
                permission.getPermissionCode(),
                permission.getPermissionName(),
                permission.getModuleName()
        );
    }

    private AdminSystemConfigVO toSystemConfigVO(SystemConfig config) {
        return new AdminSystemConfigVO(
                config.getId(),
                config.getConfigKey(),
                config.getConfigValue(),
                config.getConfigGroup(),
                config.getDescription(),
                config.getUpdatedBy(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private AdminOperationLogVO toOperationLogVO(AdminOperationLog log) {
        return new AdminOperationLogVO(
                log.getId(),
                log.getAdminUserId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetail(),
                log.getIpAddress(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }

    private void requirePermission(CurrentUser admin, String permission) {
        if (admin.permissions().contains("admin:*") || admin.permissions().contains(permission)) {
            return;
        }
        throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "缺少后台权限：" + permission);
    }

    private void writeOperationLog(
            Long adminUserId,
            String action,
            String targetType,
            Long targetId,
            Map<String, Object> detail,
            String ipAddress
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        AdminOperationLog log = new AdminOperationLog();
        log.setAdminUserId(adminUserId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(toJson(detail));
        log.setIpAddress(ipAddress);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        adminOperationLogMapper.insertLog(log);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(new LinkedHashMap<>(detail));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
