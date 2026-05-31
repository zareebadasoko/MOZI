package com.mozi.backend.global.security;

import com.mozi.backend.domain.user.entity.Role;
import com.mozi.backend.global.exception.InvalidTokenException;
import com.mozi.backend.global.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Bearer 토큰을 SecurityContext의 Authentication으로 변환하는 필터.
 *
 * 동작 흐름:
 * 1) Authorization 헤더에서 "Bearer " prefix 제거
 * 2) JwtTokenProvider로 토큰 파싱·검증 (만료/변조 → 예외)
 * 3) 검증 성공 시 AuthenticatedUser principal로 SecurityContext 채움
 * 4) 검증 실패 시 SecurityContext 비워둠 + auth.error 속성에 errorCode 기록
 *    → 보호 라우트 진입 시 EntryPoint가 해당 코드를 읽어 응답 분기
 *
 * 헤더가 없거나 Bearer 형식이 아닌 요청은 조용히 통과 — 공개 라우트는 영향
 * 없이 동작하고 보호 라우트만 EntryPoint가 401을 회신한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** request attribute key — EntryPoint가 errorCode 결정 시 읽는다. */
    public static final String AUTH_ERROR_ATTRIBUTE = "auth.error";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Claims claims = tokenProvider.parseAccessToken(token);
            Long userId = Long.parseLong(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            AuthenticatedUser principal = new AuthenticatedUser(userId, role);
            // Spring Security 권한 매퍼: ROLE_ prefix 컨벤션을 따른다 (hasRole() 매칭)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (TokenExpiredException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "TOKEN_EXPIRED");
            log.debug("Access token expired");
        } catch (InvalidTokenException e) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "INVALID_TOKEN");
            log.debug("Invalid access token");
        }

        chain.doFilter(request, response);
    }
}
// 이 클래스의 역할: 매 요청에서 Bearer 토큰을 검증하고 SecurityContext를 세팅한다.
// 잘못된 토큰일 때 즉시 응답을 쓰지 않는 이유: 공개 라우트는 토큰 없이도 통과해야 하므로
// 401 결정은 인가 단계로 위임하고 여기선 단지 errorCode만 attribute로 남긴다.
