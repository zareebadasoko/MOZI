package com.mozi.backend.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mozi.backend.global.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인가(권한) 실패 시 통일된 403 JSON 응답을 작성하는 핸들러.
 *
 * Phase 3 시점에서는 ADMIN 전용 라우트가 없어 거의 호출되지 않지만,
 * Phase 4+에서 관리자 API가 들어왔을 때를 대비해 미리 구성. 인증은 됐지만
 * 권한 부족인 경우의 표준 응답 경로를 제공한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private static final String CODE = "ACCESS_DENIED";
    private static final String MESSAGE = "이 작업을 수행할 권한이 없어요.";

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(CODE, MESSAGE));
    }
}
// 이 클래스의 역할: 인가 실패(권한 부족) 응답을 ApiResponse 형식 403 JSON으로 작성.
// EntryPoint(401)와 책임이 다르며 Spring Security가 자동으로 둘을 분기 호출한다.
