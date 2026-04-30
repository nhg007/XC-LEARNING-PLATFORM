package com.xc.study.module.auth.service;

import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import com.xc.study.module.admin.service.RuntimeConfigService;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.dto.RegisterRequest;
import com.xc.study.module.admin.mapper.AdminUserMapper;
import com.xc.study.module.classroom.mapper.ClassMemberMapper;
import com.xc.study.module.user.entity.User;
import com.xc.study.module.user.mapper.UserMapper;
import com.xc.study.module.user.mapper.UserPreferenceMapper;
import com.xc.study.security.CurrentUserProvider;
import com.xc.study.security.JwtTokenService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Test
    void loginRejectsClassTeacherOnStudentClient() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(
                userMapper,
                adminUserMapper,
                classMemberMapper,
                mock(UserPreferenceMapper.class),
                passwordEncoder,
                jwtTokenService,
                mock(CurrentUserProvider.class),
                runtimeConfigService()
        );
        User teacher = activeUser("teacher@example.com", passwordEncoder.encode("kaisa123"));

        when(userMapper.selectOne(any())).thenReturn(teacher);
        when(adminUserMapper.selectCount(any())).thenReturn(0L);
        when(classMemberMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.login(new LoginRequest("teacher@example.com", "kaisa123"))
        );

        assertEquals(ErrorCode.AUTH_CLIENT_NOT_ALLOWED, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
        verify(jwtTokenService, never()).issueToken(any());
    }

    @Test
    void loginRejectsDuplicatedBackendAccountOnStudentClient() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        ClassMemberMapper classMemberMapper = mock(ClassMemberMapper.class);
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(
                userMapper,
                adminUserMapper,
                classMemberMapper,
                mock(UserPreferenceMapper.class),
                passwordEncoder,
                jwtTokenService,
                mock(CurrentUserProvider.class),
                runtimeConfigService()
        );
        User teacher = activeUser("teacher@example.com", passwordEncoder.encode("kaisa123"));

        when(userMapper.selectOne(any())).thenReturn(teacher);
        when(adminUserMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.login(new LoginRequest("teacher@example.com", "kaisa123"))
        );

        assertEquals(ErrorCode.AUTH_CLIENT_NOT_ALLOWED, ex.getErrorCode());
        verify(userMapper, never()).updateById(any(User.class));
        verify(jwtTokenService, never()).issueToken(any());
    }

    @Test
    void registerRejectsBackendAccountOnStudentClient() {
        UserMapper userMapper = mock(UserMapper.class);
        AdminUserMapper adminUserMapper = mock(AdminUserMapper.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        AuthService service = new AuthService(
                userMapper,
                adminUserMapper,
                mock(ClassMemberMapper.class),
                mock(UserPreferenceMapper.class),
                passwordEncoder,
                mock(JwtTokenService.class),
                mock(CurrentUserProvider.class),
                runtimeConfigService()
        );

        when(adminUserMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.register(new RegisterRequest("teacher@example.com", "kaisa123", "Teacher"))
        );

        assertEquals(ErrorCode.AUTH_CLIENT_NOT_ALLOWED, ex.getErrorCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    private RuntimeConfigService runtimeConfigService() {
        return mock(RuntimeConfigService.class, invocation -> switch (invocation.getMethod().getName()) {
            case "getInt", "getLong", "getString", "getBoolean" -> invocation.getArgument(1);
            default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }

    private User activeUser(String email, String passwordHash) {
        User user = new User();
        user.setId(100L);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus("active");
        return user;
    }
}
