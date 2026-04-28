package com.xc.study.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.entity.AdminPermission;
import com.xc.study.module.admin.entity.AdminRole;
import com.xc.study.module.admin.entity.AdminRolePermission;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.entity.AdminUserRole;
import com.xc.study.module.admin.mapper.AdminPermissionMapper;
import com.xc.study.module.admin.mapper.AdminRoleMapper;
import com.xc.study.module.admin.mapper.AdminRolePermissionMapper;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.admin.mapper.AdminUserRoleMapper;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.vo.AdminProfileVO;
import com.xc.study.module.auth.vo.AuthTokenVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import com.xc.study.security.JwtTokenService;
import com.xc.study.security.UserType;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            AdminUserRoleMapper adminUserRoleMapper,
            AdminRoleMapper adminRoleMapper,
            AdminRolePermissionMapper adminRolePermissionMapper,
            AdminPermissionMapper adminPermissionMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminUserMapper = adminUserMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminRolePermissionMapper = adminRolePermissionMapper;
        this.adminPermissionMapper = adminPermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AuthTokenVO<AdminProfileVO> login(LoginRequest request) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, normalizeAccount(request.account()))
                .last("limit 1"));
        if (admin == null || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "账号或密码错误");
        }
        if (!"active".equals(admin.getStatus())) {
            throw BusinessException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "管理员已被禁用");
        }
        admin.setLastLoginAt(OffsetDateTime.now());
        adminUserMapper.updateById(admin);
        return issueAdminToken(admin);
    }

    public AdminProfileVO currentProfile() {
        CurrentUser currentUser = currentUserProvider.requireAdmin();
        AdminUser admin = adminUserMapper.selectById(currentUser.id());
        if (admin == null) {
            throw BusinessException.notFound("管理员不存在");
        }
        return toProfile(admin);
    }

    private AuthTokenVO<AdminProfileVO> issueAdminToken(AdminUser admin) {
        AdminAccess access = loadAdminAccess(admin.getId());
        CurrentUser currentUser = new CurrentUser(
                admin.getId(),
                admin.getUsername(),
                UserType.ADMIN,
                access.roles(),
                access.permissions()
        );
        return new AuthTokenVO<>("Bearer", jwtTokenService.issueToken(currentUser), toProfile(admin, access));
    }

    private AdminProfileVO toProfile(AdminUser admin) {
        return toProfile(admin, loadAdminAccess(admin.getId()));
    }

    private AdminProfileVO toProfile(AdminUser admin, AdminAccess access) {
        return new AdminProfileVO(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getStatus(),
                access.roles(),
                access.permissions()
        );
    }

    private AdminAccess loadAdminAccess(Long adminUserId) {
        List<AdminUserRole> userRoles = adminUserRoleMapper.selectList(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminUserId));
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        List<Long> roleIds = userRoles.stream()
                .map(AdminUserRole::getRoleId)
                .distinct()
                .toList();
        if (roleIds.isEmpty()) {
            return new AdminAccess(roles, Set.of());
        }
        Map<Long, AdminRole> rolesById = adminRoleMapper.selectBatchIds(roleIds)
                .stream()
                .collect(Collectors.toMap(AdminRole::getId, Function.identity()));
        roles.addAll(roleIds.stream()
                .map(rolesById::get)
                .filter(role -> role != null && role.getRoleCode() != null)
                .map(AdminRole::getRoleCode)
                .collect(Collectors.toSet()));
        List<AdminRolePermission> rolePermissions = adminRolePermissionMapper.selectList(
                new LambdaQueryWrapper<AdminRolePermission>()
                        .in(AdminRolePermission::getRoleId, roleIds));
        List<Long> permissionIds = rolePermissions.stream()
                .map(AdminRolePermission::getPermissionId)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return new AdminAccess(roles, Set.of());
        }
        Map<Long, AdminPermission> permissionsById = adminPermissionMapper.selectBatchIds(permissionIds)
                .stream()
                .collect(Collectors.toMap(AdminPermission::getId, Function.identity()));
        Set<String> permissions = permissionIds.stream()
                .map(permissionsById::get)
                .filter(permission -> permission != null && permission.getPermissionCode() != null)
                .map(AdminPermission::getPermissionCode)
                .collect(Collectors.toUnmodifiableSet());
        return new AdminAccess(roles, permissions);
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase(Locale.ROOT);
    }

    private record AdminAccess(Set<String> roles, Set<String> permissions) {
    }
}
