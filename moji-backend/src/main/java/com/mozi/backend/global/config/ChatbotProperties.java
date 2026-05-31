package com.mozi.backend.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 챗봇 서버 연동에 사용되는 설정값 모음.
 *
 * application.yml의 `mozi.chatbot.*` 키에 바인딩된다. 본 phase는 Mock 우선이라
 * `mockEnabled=true`일 때는 baseUrl/apiKey가 비어 있어도 부팅이 정상 진행되어야 하므로
 * 두 필드의 @NotBlank는 적용하지 않는다. 실 구현체(RestChatbotClient)가 활성화될 때만
 * 호출 시점에 값이 채워졌는지 본인 책임으로 확인.
 *
 * @param mockEnabled Mock 구현체 사용 여부. true면 MockChatbotClient 빈이 등록되고
 *                    false면 실 구현체(RestChatbotClient)가 등록됨. 본 phase는 후자 미준비.
 * @param baseUrl 챗봇 서버의 기본 URL. 실 구현체 활성 시 필수 (예: https://chatbot.internal/api)
 * @param timeoutSeconds 챗봇 호출 타임아웃 (초). 8초 권장. 초과 시 CHATBOT_TIMEOUT 변환
 * @param apiKey 챗봇 서버 호출 시 사용할 API 키. X-API-Key 헤더로 전송.
 *               환경변수 CHATBOT_API_KEY로 주입 권장. dev에선 빈값 허용.
 */
@Validated
@ConfigurationProperties(prefix = "mozi.chatbot")
public record ChatbotProperties(
        boolean mockEnabled,
        String baseUrl,
        @Positive long timeoutSeconds,
        String apiKey
) {
}
// 이 record의 역할: 챗봇 연동 설정을 타입 안전하게 한 곳에 모은다.
// baseUrl/apiKey는 Mock 모드에서 비어도 무방하도록 @NotBlank를 의도적으로 생략 — 실 구현체 활성 시 호출 측에서 확인.
