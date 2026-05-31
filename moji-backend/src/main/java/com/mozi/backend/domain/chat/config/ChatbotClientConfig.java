package com.mozi.backend.domain.chat.config;

import com.mozi.backend.global.config.ChatbotProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 챗봇 서버 실 호출용 RestClient 빈 등록 — `mozi.chatbot.mock-enabled=false`일 때만 활성.
 *
 * 본 phase(Phase 5)에선 실 구현체(`RestChatbotClient`)가 아직 미구현이라 빈 등록도 의도적으로
 * 보류한다. Mock 비활성 + 실 구현체 미준비 상태에서 부팅 시 `NoSuchBeanDefinitionException:
 * ChatbotClient` 발생 → 명확한 에러로 "운영 환경에서 챗봇 미준비" 상황을 즉시 인지.
 *
 * 향후 별도 작업(`RestChatbotClient` 구현 시) 본 파일의 TODO 주석 부분을 채워:
 * - `restChatbotClient` 빈 정의
 * - `RestClient` 빌더에서 baseUrl + defaultHeader(X-API-Key) + 타임아웃 설정
 * - 챗봇 팀 합의 후 SLA 변경 시 timeoutSeconds 재조정
 */
@Configuration
@ConditionalOnProperty(name = "mozi.chatbot.mock-enabled", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
public class ChatbotClientConfig {

    private final ChatbotProperties properties;

    /**
     * 챗봇 서버 호출 전용 RestClient 빈.
     *
     * `RestClient.builder()`로 baseUrl + 기본 헤더 + 타임아웃 설정. 단,
     * `SimpleClientHttpRequestFactory`는 connect/read timeout 모두 동일하게 적용.
     * 챗봇 서버 SLA에 따라 timeout 분리가 필요해지면 `JdkClientHttpRequestFactory`로 변경 검토.
     *
     * @return 챗봇 서버 호출용 RestClient (X-API-Key 헤더 + 타임아웃 적용)
     */
    @Bean
    public RestClient chatbotRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey())
                .requestFactory(requestFactory)
                .build();
    }

    // TODO Phase 5 후속 작업: RestChatbotClient 구현체 추가 + 본 Config에 빈 정의
    // @Bean
    // public ChatbotClient restChatbotClient(RestClient chatbotRestClient) {
    //     return new RestChatbotClient(chatbotRestClient);
    // }
}
// 이 클래스의 역할: 실 챗봇 서버 호출 빈 등록 (mock-enabled=false 활성).
// Phase 5에선 RestClient 빈만 등록 — ChatbotClient 구현체는 후속 작업에서 추가.
// Mock + 실 모두 없으면 부팅 실패 (의도된 안전망 — 운영에서 챗봇 미준비 상태 즉시 발견).
