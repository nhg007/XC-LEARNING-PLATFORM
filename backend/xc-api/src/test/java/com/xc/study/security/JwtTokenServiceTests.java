package com.xc.study.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTests {

    @Test
    void issueAndParseToken() {
        JwtTokenService tokenService = new JwtTokenService(
                new ObjectMapper(),
                "test-secret-with-at-least-32-characters",
                "xc-learning-test",
                30
        );
        CurrentUser currentUser = new CurrentUser(
                1001L,
                "student@example.com",
                UserType.STUDENT,
                Set.of("student"),
                Set.of("student:self")
        );

        JwtClaims claims = tokenService.parse(tokenService.issueToken(currentUser));

        assertEquals(1001L, claims.subjectId());
        assertEquals("student@example.com", claims.account());
        assertEquals(UserType.STUDENT, claims.type());
        assertTrue(claims.permissions().contains("student:self"));
        assertTrue(claims.expiresAt().isAfter(java.time.Instant.now()));
    }
}
