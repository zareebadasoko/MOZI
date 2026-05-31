package com.mozi.backend.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /api/chat 요청 DTO (클라이언트 → 백엔드).
 *
 * `conversationId`는 nullable — 첫 요청은 비워서 보내면 백엔드가 UUID v4 발급, 후속 요청은
 * sessionStorage에 보관한 ID를 그대로 재전송. message 최대 1000자 제한은 챗봇 서버 부담 + UX
 * 측면(노년층이 너무 긴 메시지를 받지 않게)에서 적당히 큰 값으로 둠.
 *
 * @param message 사용자 메시지 (자연어, 1~1000자)
 * @param conversationId 백엔드가 발급한 대화 ID (첫 요청은 null/blank)
 */
public record ChatRequest(
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 1000, message = "메시지가 너무 길어요.")
        String message,
        String conversationId
) {
}
// 이 record의 역할: 챗봇 호출 시 사용자 메시지 + 대화 컨텍스트 ID를 받는다.
// conversationId 정책 — 첫 요청은 null, 후속은 이전 응답에서 받은 ID 재사용.
