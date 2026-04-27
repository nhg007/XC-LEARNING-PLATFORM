package com.xc.study.security;

import java.time.Instant;
import java.util.Set;

public record JwtClaims(
        Long subjectId,
        String account,
        UserType type,
        Set<String> roles,
        Set<String> permissions,
        Instant expiresAt
) {
}
