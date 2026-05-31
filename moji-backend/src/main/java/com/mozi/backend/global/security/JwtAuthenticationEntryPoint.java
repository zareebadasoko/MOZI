package com.mozi.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 통일된 401 JSON 응답을 작성하는 EntryPoint.
 *
 * JwtAuthenticationFilter가 request에 남긴 auth.error attribute를 읽어
 * errorCode를 결정한다. attribute가 없으면(헤더 자체가 없는 경우) 기본
 * UNAUTHORIZED로 응답.
 *
 * GlobalExceptionHandler가 컨트롤러 예외만 잡기 때문에, 필터 체인에서
 * 발생한 인증 실패는 별도로 여기서 처리해야 한다 — 그래야 ApiResponse 형식이 깨지지 않는다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = (String) request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (code == null) {
            code = "UNAUTHORIZED";
        }

        String message = switch (code) {
            case "TOKEN_EXPIRED" -> "로그인 세션이 만료되었어요. 다시 로그인해주세요.";
            case "INVALID_TOKEN" -> "유효하지 않은 인증 정보예요.";
            default -> "로그인이 필요해요.";
        };

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(code, message));
    }
}
// 이 클래스의 역할: 인증 단계 실패를 ApiResponse 형식 JSON으로 응답.
// 필터에서 이미 errorCode를 정해 두었으므로 여기선 메시지만 매핑해 1:1로 응답한다.
