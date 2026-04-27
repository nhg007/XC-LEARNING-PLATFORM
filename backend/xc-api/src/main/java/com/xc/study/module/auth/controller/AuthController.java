package com.xc.study.module.auth.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.dto.RegisterRequest;
import com.xc.study.module.auth.service.AuthService;
import com.xc.study.module.auth.vo.AuthTokenVO;
import com.xc.study.module.auth.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthTokenVO<UserProfileVO>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenVO<UserProfileVO>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileVO> me() {
        return ApiResponse.ok(authService.currentProfile());
    }
}
