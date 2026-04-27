package com.xc.study.module.auth.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.auth.dto.LoginRequest;
import com.xc.study.module.auth.service.AdminAuthService;
import com.xc.study.module.auth.vo.AdminProfileVO;
import com.xc.study.module.auth.vo.AuthTokenVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenVO<AdminProfileVO>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminProfileVO> me() {
        return ApiResponse.ok(adminAuthService.currentProfile());
    }
}
