package com.xc.study.security;

import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public CurrentUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw BusinessException.unauthorized(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return currentUser;
    }

    public CurrentUser requireStudent() {
        CurrentUser currentUser = requireUser();
        if (!currentUser.isStudent()) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "当前账号不能访问学生端接口");
        }
        return currentUser;
    }

    public CurrentUser requireAdmin() {
        CurrentUser currentUser = requireUser();
        if (!currentUser.isAdmin()) {
            throw BusinessException.forbidden(ErrorCode.FORBIDDEN, "当前账号不能访问后台接口");
        }
        return currentUser;
    }
}
