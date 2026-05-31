package com.mozi.backend.domain.chat.client.dto;

/**
 * 백엔드 → 챗봇 서버 호출의 Request 페이로드 (외부 API 계약).
 *
 * 본 record는 우리가 챗봇 팀과 합의한 외부 API 명세(`docs/CHATBOT_API_CONTRACT.md` §4-2)와
 * 1:1로 매핑된다. 필드/필드명을 임의로 변경하면 계약 위반이므로, 변경 시 docs와 동시에 갱신.
 *
 * @param message 사용자 자연어 메시지 (1~1000자)
 * @param conversationId 대화 컨텍스트 ID (UUID v4, 백엔드 발급)
 * @param user 사용자 컨텍스트 (userId + 12 필드 프로필)
 */
public record ChatbotRequest(
        String message,
        String conversationId,
        ChatbotUserContext user
) {
}
// 이 record의 역할: 챗봇 서버 호출의 외부 Request 페이로드.
// 이름·필드 변경은 docs/CHATBOT_API_CONTRACT.md와 동시 갱신해야 한다.
