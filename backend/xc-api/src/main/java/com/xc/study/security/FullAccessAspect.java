package com.xc.study.security;

import com.xc.study.module.membership.service.MembershipService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FullAccessAspect {

    private final CurrentUserProvider currentUserProvider;
    private final MembershipService membershipService;

    public FullAccessAspect(CurrentUserProvider currentUserProvider, MembershipService membershipService) {
        this.currentUserProvider = currentUserProvider;
        this.membershipService = membershipService;
    }

    @Before("@within(com.xc.study.security.RequireFullAccess) || @annotation(com.xc.study.security.RequireFullAccess)")
    public void requireFullAccess() {
        CurrentUser currentUser = currentUserProvider.requireStudent();
        membershipService.requireFullAccess(currentUser.id());
    }
}
