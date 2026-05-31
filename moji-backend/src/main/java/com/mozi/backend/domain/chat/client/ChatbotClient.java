package com.mozi.backend.domain.chat.client;

import com.mozi.backend.domain.chat.client.dto.ChatbotRequest;
import com.mozi.backend.domain.chat.client.dto.ChatbotResponse;
import com.mozi.backend.domain.chat.exception.ChatbotInvalidResponseException;
import com.mozi.backend.domain.chat.exception.ChatbotTimeoutException;
import com.mozi.backend.domain.chat.exception.ChatbotUnavailableException;

/**
 * 챗봇 서버 호출 추상화 인터페이스.
 *
 * 두 구현체 중 하나가 `@ConditionalOnProperty(mozi.chatbot.mock-enabled)` 토글에 따라 활성화:
 *  - `MockChatbotClient` (mock-enabled=true): 시드 복지 ID 중 무작위 추천. 시연·테스트용.
 *  - `RestChatbotClient` (mock-enabled=false): 실 챗봇 서버를 RestClient로 호출. 본 phase에선 미구현.
 *
 * 호출자(ChatService)는 본 인터페이스만 의존하므로 두 구현체 전환이 코드 변경 없이 가능.
 * 호출 실패(타임아웃·5xx·스키마 오류)는 모두 도메인 예외로 변환되어 GlobalExceptionHandler가
 * 통일된 ApiResponse로 응답.
 */
public interface ChatbotClient {

    /**
     * 챗봇 서버에 요청을 보내고 응답을 받는다.
     *
     * @param request message + conversationId + user 컨텍스트
     * @return 답변 텍스트 + 추천 복지 ID 목록 + conversationId
     * @throws ChatbotTimeoutException 응답 시간 초과 (504 매핑)
     * @throws ChatbotUnavailableException 챗봇 서버 5xx 또는 연결 실패 (503 매핑)
     * @throws ChatbotInvalidResponseException 응답 스키마 위반 (502 매핑)
     */
    ChatbotResponse send(ChatbotRequest request);
}
// 이 인터페이스의 역할: Mock/실 구현체 전환의 단일 의존 지점.
// 호출 실패는 모두 명시적 도메인 예외 3종으로 분류해 클라이언트가 분기 가능하도록 함.
