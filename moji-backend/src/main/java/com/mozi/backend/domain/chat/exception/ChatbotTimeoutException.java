package com.mozi.backend.domain.chat.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 챗봇 서버가 약속된 응답 시간(`mozi.chatbot.timeout-seconds`, 기본 8초)을 초과한 경우.
 *
 * LLM 추론 특성상 응답이 지연될 수 있어 504 Gateway Timeout으로 안내한다.
 * 사용자에게는 일시적 문제로 보이도록 친화 메시지 노출.
 */
public class ChatbotTimeoutException extends BusinessException {

    private static final String CODE = "CHATBOT_TIMEOUT";
    private static final String MESSAGE = "지금은 추천이 어려워요. 잠시 후 다시 시도해주세요.";

    public ChatbotTimeoutException() {
        super(CODE, MESSAGE, HttpStatus.GATEWAY_TIMEOUT);
    }
}
// 이 클래스의 역할: 챗봇 호출 타임아웃을 도메인 예외로 표현 + 사용자 친화 메시지.
