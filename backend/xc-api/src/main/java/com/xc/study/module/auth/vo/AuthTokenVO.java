package com.xc.study.module.auth.vo;

public record AuthTokenVO<T>(
        String tokenType,
        String accessToken,
        T profile
) {
}
