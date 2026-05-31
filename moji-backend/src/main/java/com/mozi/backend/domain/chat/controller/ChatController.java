package com.mozi.backend.domain.chat.controller;

import com.mozi.backend.domain.chat.dto.ChatRequest;
import com.mozi.backend.domain.chat.dto.ChatResponse;
import com.mozi.backend.domain.chat.service.ChatService;
import com.mozi.backend.global.common.ApiResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 챗봇 브릿지 엔드포인트(`POST /api/chat`)의 단일 진입점.
 *
 * 인증 필수 — SecurityConfig의 `.anyRequest().authenticated()`가 자동 강제. 미인증 시
 * JwtAuthenticationEntryPoint가 401 UNAUTHORIZED 응답. 비즈니스 로직은 ChatService가
 * 담당하며 컨트롤러는 라우팅·검증·응답 래핑만.
 */
@Tag(name = "Chat", description = "챗봇 브릿지 — 사용자 메시지를 외부 챗봇 서버로 전달 + 추천 복지 hydrate (인증 필수)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 챗봇 호출.
     *
     * @param request 사용자 메시지 + 선택적 conversationId (첫 요청은 null)
     * @param principal SecurityContext의 인증 사용자
     * @return 답변 텍스트 + 추천 복지 목록 + conversationId (sessionStorage 보관용)
     */
    @Operation(summary = "챗봇 호출", description = "conversationId 비우면 UUID v4 자동 발급. 응답의 recommendedWelfareIds가 [] 빈 배열일 수 있음(인사·잡담 등).")
    @PostMapping
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(chatService.chat(principal.userId(), request));
    }
}
// 이 클래스의 역할: HTTP ↔ ChatService 어댑터.
// 챗봇 호출 실패(타임아웃·5xx·스키마 오류)는 도메인 예외 3종으로 GlobalExceptionHandler가 변환.
