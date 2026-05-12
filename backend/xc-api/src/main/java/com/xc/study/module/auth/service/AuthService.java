package com.xc.study.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.service.RuntimeConfigService;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.dto.RegisterRequest;
import com.xc.study.module.auth.vo.AuthTokenVO;
import com.xc.study.module.auth.vo.UserProfileVO;
import com.xc.study.module.admin.entity.AdminUser;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.entity.UserPreference;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
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
public class AuthService {

    private static final int TRIAL_DAYS = 7;

    private final UserMapper userMapper;
    private final AdminUserMapper adminUserMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserProvider currentUserProvider;
    private final RuntimeConfigService runtimeConfigService;

    public AuthService(
            UserMapper userMapper,
            AdminUserMapper adminUserMapper,
            UserPreferenceMapper userPreferenceMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            CurrentUserProvider currentUserProvider,
            RuntimeConfigService runtimeConfigService
    ) {
        this.userMapper = userMapper;
        this.adminUserMapper = adminUserMapper;
        this.userPreferenceMapper = userPreferenceMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.currentUserProvider = currentUserProvider;
        this.runtimeConfigService = runtimeConfigService;
    }

    @Transactional
    public AuthTokenVO<UserProfileVO> register(RegisterRequest request) {
        String email = normalizeAccount(request.email());
        if (isBackendAccount(email)) {
            throw BusinessException.forbidden(ErrorCode.AUTH_CLIENT_NOT_ALLOWED, "后台账号不能注册学生端");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (exists > 0) {
            throw BusinessException.conflict("邮箱已注册");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname());
        user.setStatus("active");
        user.setTrialStartedAt(now);
        user.setTrialEndsAt(now.plusDays(runtimeConfigService.getInt(RuntimeConfigService.MEMBERSHIP_TRIAL_DAYS, TRIAL_DAYS)));
        userMapper.insert(user);

        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        preference.setUiLanguage("zh");
        preference.setTranslationLanguage("ru");
        preference.setVocabMeaningLanguage("ru");
        preference.setMatchingMeaningLanguage("ru");
        preference.setSoundEnabled(true);
        userPreferenceMapper.insert(preference);

        return issueStudentToken(user);
    }

    @Transactional
    public AuthTokenVO<UserProfileVO> login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, normalizeAccount(request.account()))
                .last("limit 1"));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_INVALID_CREDENTIALS, "账号或密码错误");
        }
        if (!"active".equals(user.getStatus())) {
            throw BusinessException.forbidden(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被禁用");
        }
        if (isBackendAccount(user.getEmail())) {
            throw BusinessException.forbidden(ErrorCode.AUTH_CLIENT_NOT_ALLOWED, "后台账号请登录后台管理端");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        userMapper.updateById(user);
        return issueStudentToken(user);
    }

    public UserProfileVO currentProfile() {
        CurrentUser currentUser = currentUserProvider.requireStudent();
        User user = userMapper.selectById(currentUser.id());
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return toProfile(user);
    }

    private AuthTokenVO<UserProfileVO> issueStudentToken(User user) {
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getEmail(), UserType.STUDENT, Set.of("student"), Set.of("student:self"));
        return new AuthTokenVO<>("Bearer", jwtTokenService.issueToken(currentUser), toProfile(user));
    }

    private boolean isBackendAccount(String email) {
        if (email == null) {
            return false;
        }
        return adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, email.trim().toLowerCase(Locale.ROOT))) > 0;
    }

    private UserProfileVO toProfile(User user) {
        return new UserProfileVO(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus(),
                user.getTrialStartedAt(),
                user.getTrialEndsAt()
        );
    }

    private String normalizeAccount(String account) {
        return account.trim().toLowerCase(Locale.ROOT);
    }
}
