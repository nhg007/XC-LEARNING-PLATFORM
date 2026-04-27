package com.xc.study.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.vo.AdminProfileVO;
import com.xc.study.module.auth.vo.AuthTokenVO;
import com.xc.study.security.CurrentUser;
import com.xc.study.security.CurrentUserProvider;
import com.xc.study.security.JwtTokenService;
import com.xc.study.security.UserType;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            CurrentUserProvider currentUserProvider
    ) {
        this.adminUserMapper = adminUserMapper;
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
        CurrentUser currentUser = new CurrentUser(admin.getId(), admin.getUsername(), UserType.ADMIN, Set.of("admin"), Set.of("admin:*"));
        return new AuthTokenVO<>("Bearer", jwtTokenService.issueToken(currentUser), toProfile(admin));
    }

    private AdminProfileVO toProfile(AdminUser admin) {
        return new AdminProfileVO(
                admin.getId(),
                admin.getUsername(),
                admin.getDisplayName(),
                admin.getStatus(),
                Set.of("admin"),
                Set.of("admin:*")
        );
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase(Locale.ROOT);
    }
}
