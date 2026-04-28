package com.xc.study.module.user.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.user.dto.UpdateUserPreferenceRequest;
import com.xc.study.module.user.service.UserPreferenceService;
import com.xc.study.module.user.vo.UserPreferenceVO;
import com.xc.study.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/preferences")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final CurrentUserProvider currentUserProvider;

    public UserPreferenceController(UserPreferenceService userPreferenceService, CurrentUserProvider currentUserProvider) {
        this.userPreferenceService = userPreferenceService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<UserPreferenceVO> getPreference() {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(userPreferenceService.getPreference(userId));
    }

    @PutMapping
    public ApiResponse<UserPreferenceVO> updatePreference(@Valid @RequestBody UpdateUserPreferenceRequest request) {
        Long userId = currentUserProvider.requireStudent().id();
        return ApiResponse.ok(userPreferenceService.updatePreference(userId, request));
    }
}
