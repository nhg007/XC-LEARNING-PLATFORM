package com.xc.study.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String secret;
    private final String issuer;
    private final long expiresMinutes;

    public JwtTokenService(
            ObjectMapper objectMapper,
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.expires-minutes}") long expiresMinutes
    ) {
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.secret = secret;
        this.issuer = issuer;
        this.expiresMinutes = expiresMinutes;
    }

    public String issueToken(CurrentUser currentUser) {
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT 密钥未配置");
        }
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(expiresMinutes * 60);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", issuer);
        payload.put("sub", currentUser.id().toString());
        payload.put("account", currentUser.account());
        payload.put("type", currentUser.type().name());
        payload.put("roles", currentUser.roles());
        payload.put("permissions", currentUser.permissions());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signingInput = encodedHeader + "." + encodedPayload;
        return signingInput + "." + sign(signingInput);
    }

    public JwtClaims parse(String token) {
        if (!StringUtils.hasText(secret)) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Token 配置不可用");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Token 格式错误");
        }
        String signingInput = parts[0] + "." + parts[1];
        if (!constantTimeEquals(sign(signingInput), parts[2])) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Token 签名无效");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        if (!issuer.equals(payload.get("iss"))) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Token 签发方无效");
        }
        Instant expiresAt = Instant.ofEpochSecond(readLong(payload, "exp"));
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_EXPIRED, "登录已过期");
        }

        return new JwtClaims(
                Long.valueOf(String.valueOf(payload.get("sub"))),
                String.valueOf(payload.get("account")),
                UserType.valueOf(String.valueOf(payload.get("type"))),
                readStringSet(payload.get("roles")),
                readStringSet(payload.get("permissions")),
                expiresAt
        );
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Token 生成失败");
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(BASE64_URL_DECODER.decode(value), MAP_TYPE);
        } catch (Exception ex) {
            throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "Token 内容无效");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Token 签名失败");
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private long readLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Set<String> readStringSet(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        }
        return Set.of();
    }
}
