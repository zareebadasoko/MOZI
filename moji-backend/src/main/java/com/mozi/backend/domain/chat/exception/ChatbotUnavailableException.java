package com.mozi.backend.domain.chat.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 챗봇 서버 자체가 5xx 응답 또는 연결 실패(네트워크 단절, DNS 실패 등)인 경우.
 *
 * 503 Service Unavailable로 안내해 챗봇 의존 서비스가 일시 중단되었음을 명확히 한다.
 * 타임아웃(`CHATBOT_TIMEOUT`)과는 다른 코드로 구분 — 클라이언트가 로그/재시도 정책을
 * 다르게 적용할 수 있도록.
 */
public class ChatbotUnavailableException extends BusinessException {

    private static final String CODE = "CHATBOT_UNAVAILABLE";
    private static final String MESSAGE = "챗봇 서버가 일시적으로 응답하지 않아요.";

    public ChatbotUnavailableException() {
        super(CODE, MESSAGE, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
// 이 클래스의 역할: 챗봇 서버 측 장애(5xx/연결 실패)를 도메인 예외로 표현.
