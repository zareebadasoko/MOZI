package com.mozi.backend.domain.chat.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 챗봇 서버 응답이 약속된 스키마와 맞지 않을 때 (JSON 파싱 실패, 필수 필드 누락 등).
 *
 * 502 Bad Gateway로 안내 — 챗봇은 동작 중이나 응답 형식 자체가 잘못된 경우.
 * 챗봇 팀이 외부 계약(`docs/CHATBOT_API_CONTRACT.md`)을 깬 케이스이므로 서버 로그에 상세 기록 필요.
 */
public class ChatbotInvalidResponseException extends BusinessException {

    private static final String CODE = "CHATBOT_INVALID_RESPONSE";
    private static final String MESSAGE = "챗봇 응답에 문제가 있어요. 잠시 후 다시 시도해주세요.";

    public ChatbotInvalidResponseException() {
        super(CODE, MESSAGE, HttpStatus.BAD_GATEWAY);
    }
}
// 이 클래스의 역할: 챗봇 응답 스키마 위반을 도메인 예외로 표현 (계약 위반 시그널).
