package com.xc.study.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xc.study.common.ApiResponse;
import com.xc.study.common.BusinessException;
import com.xc.study.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(header)) {
                if (!header.startsWith("Bearer ")) {
                    throw BusinessException.unauthorized(ErrorCode.AUTH_TOKEN_INVALID, "认证头格式错误");
                }
                JwtClaims claims = jwtTokenService.parse(header.substring(7));
                CurrentUser currentUser = new CurrentUser(
                        claims.subjectId(),
                        claims.account(),
                        claims.type(),
                        claims.roles(),
                        claims.permissions()
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(currentUser, null, currentUser.permissions().stream()
                                .map(permission -> (org.springframework.security.core.GrantedAuthority) () -> permission)
                                .toList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(ex.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail(ex.getErrorCode(), ex.getMessage()));
        }
    }
}
