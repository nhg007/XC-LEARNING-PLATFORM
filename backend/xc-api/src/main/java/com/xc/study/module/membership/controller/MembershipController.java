package com.xc.study.module.membership.controller;

import com.xc.study.common.ApiResponse;
import com.xc.study.module.membership.service.MembershipService;
import com.xc.study.module.membership.vo.MembershipPlanVO;
import com.xc.study.module.membership.vo.MembershipStatusVO;
import com.xc.study.security.CurrentUserProvider;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/membership")
public class MembershipController {

    private final MembershipService membershipService;
    private final CurrentUserProvider currentUserProvider;

    public MembershipController(MembershipService membershipService, CurrentUserProvider currentUserProvider) {
        this.membershipService = membershipService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/status")
    public ApiResponse<MembershipStatusVO> status() {
        return ApiResponse.ok(membershipService.getStatus(currentUserProvider.requireStudent().id()));
    }

    @GetMapping("/plans")
    public ApiResponse<List<MembershipPlanVO>> plans() {
        currentUserProvider.requireStudent();
        return ApiResponse.ok(membershipService.listActivePlans());
    }
}
