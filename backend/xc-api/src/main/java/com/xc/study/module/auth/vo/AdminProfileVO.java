package com.xc.study.module.auth.vo;

import java.util.Set;

public record AdminProfileVO(
        Long id,
        String username,
        String displayName,
        String status,
        Set<String> roles,
        Set<String> permissions
) {
}
