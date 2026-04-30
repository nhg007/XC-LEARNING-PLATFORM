package com.xc.study.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    private static final String ADMIN_ALL_PERMISSION_CODE = "admin:*";

    private final AdminUserMapper adminUserMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminPermissionMapper adminPermissionMapper;
    private final AdminRolePermissionMapper adminRolePermissionMapper;
    private final AdminUserRoleMapper adminUserRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String displayName;

    public AdminBootstrapRunner(
            AdminUserMapper adminUserMapper,
            AdminRoleMapper adminRoleMapper,
            AdminPermissionMapper adminPermissionMapper,
            AdminRolePermissionMapper adminRolePermissionMapper,
            AdminUserRoleMapper adminUserRoleMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.password:}") String password,
            @Value("${app.bootstrap.admin.display-name:Local Admin}") String displayName
    ) {
        this.adminUserMapper = adminUserMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminPermissionMapper = adminPermissionMapper;
        this.adminRolePermissionMapper = adminRolePermissionMapper;
        this.adminUserRoleMapper = adminUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("Admin bootstrap is enabled but username or password is blank.");
            return;
        }
        if (isPlaceholder(username) || isPlaceholder(password)) {
            log.warn("Admin bootstrap username or password still uses placeholder value.");
            return;
        }
        if (password.length() < 12) {
            log.warn("Admin bootstrap password must be at least 12 characters.");
            return;
        }

        AdminUser existing = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            ensureSuperAdminAccess(existing, now);
            log.info("Admin user '{}' already exists, ensured super admin access and skipped password update.", username);
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setDisplayName(StringUtils.hasText(displayName) ? displayName : username);
        admin.setStatus("active");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        adminUserMapper.insert(admin);
        ensureSuperAdminAccess(admin, now);
        log.info("Created bootstrap admin user '{}'.", username);
    }

    private boolean isPlaceholder(String value) {
        return value != null && value.trim().toLowerCase().contains("replace_");
    }

    private void ensureSuperAdminAccess(AdminUser admin, OffsetDateTime now) {
        AdminPermission permission = ensureAdminAllPermission(now);
        AdminRole role = ensureSuperAdminRole(now);
        ensureRolePermission(role.getId(), permission.getId(), now);
        ensureAdminRole(admin.getId(), role.getId(), now);
    }

    private AdminPermission ensureAdminAllPermission(OffsetDateTime now) {
        AdminPermission existing = adminPermissionMapper.selectOne(new LambdaQueryWrapper<AdminPermission>()
                .eq(AdminPermission::getPermissionCode, ADMIN_ALL_PERMISSION_CODE));
        if (existing != null) {
            return existing;
        }
        AdminPermission permission = new AdminPermission();
        permission.setPermissionCode(ADMIN_ALL_PERMISSION_CODE);
        permission.setPermissionName("后台全部权限");
        permission.setModuleName("system");
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);
        adminPermissionMapper.insert(permission);
        return permission;
    }

    private AdminRole ensureSuperAdminRole(OffsetDateTime now) {
        AdminRole existing = adminRoleMapper.selectOne(new LambdaQueryWrapper<AdminRole>()
                .eq(AdminRole::getRoleCode, SUPER_ADMIN_ROLE_CODE));
        if (existing != null) {
            return existing;
        }
        AdminRole role = new AdminRole();
        role.setRoleCode(SUPER_ADMIN_ROLE_CODE);
        role.setRoleName("超级管理员");
        role.setDescription("拥有后台全部权限");
        role.setCreatedAt(now);
        role.setUpdatedAt(now);
        adminRoleMapper.insert(role);
        return role;
    }

    private void ensureRolePermission(Long roleId, Long permissionId, OffsetDateTime now) {
        AdminRolePermission existing = adminRolePermissionMapper.selectOne(new LambdaQueryWrapper<AdminRolePermission>()
                .eq(AdminRolePermission::getRoleId, roleId)
                .eq(AdminRolePermission::getPermissionId, permissionId));
        if (existing != null) {
            return;
        }
        AdminRolePermission relation = new AdminRolePermission();
        relation.setRoleId(roleId);
        relation.setPermissionId(permissionId);
        relation.setCreatedAt(now);
        relation.setUpdatedAt(now);
        adminRolePermissionMapper.insert(relation);
    }

    private void ensureAdminRole(Long adminUserId, Long roleId, OffsetDateTime now) {
        AdminUserRole existing = adminUserRoleMapper.selectOne(new LambdaQueryWrapper<AdminUserRole>()
                .eq(AdminUserRole::getAdminUserId, adminUserId)
                .eq(AdminUserRole::getRoleId, roleId));
        if (existing != null) {
            return;
        }
        AdminUserRole relation = new AdminUserRole();
        relation.setAdminUserId(adminUserId);
        relation.setRoleId(roleId);
        relation.setCreatedAt(now);
        relation.setUpdatedAt(now);
        adminUserRoleMapper.insert(relation);
    }
}
