package com.xc.study.security;

import java.util.Set;

public record CurrentUser(
        Long id,
        String account,
        UserType type,
        Set<String> roles,
        Set<String> permissions
) {
    public boolean isAdmin() {
        return type == UserType.ADMIN;
    }

    public boolean isStudent() {
        return type == UserType.STUDENT;
    }
}
