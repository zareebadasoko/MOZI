package com.mozi.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 애플리케이션 전역에서 공유하는 인프라 빈을 모아둔 설정 클래스.
 *
 * SecurityConfig 안에 Clock 빈을 두면 다음 순환 참조가 발생한다:
 * SecurityConfig → JwtAuthenticationFilter → JwtTokenProvider → Clock(SecurityConfig)
 * Clock을 별도 Configuration으로 분리하면 의존 그래프가 비순환이 된다.
 *
 * 향후 ObjectMapper 커스터마이징, 공통 RestTemplate 등 도메인 무관 빈도 여기 추가.
 */
@Configuration
public class AppConfig {

    /**
     * 시간 의존성 주입용 Clock 빈.
     *
     * AuthService와 JwtTokenProvider가 직접 Instant.now() / LocalDateTime.now() 를 호출하면
     * 단위 테스트에서 시간 고정이 어렵다. Clock을 주입받게 해서 테스트에서는
     * Clock.fixed(...)로 만료 직전/직후 분기를 결정적으로 검증할 수 있게 한다.
     *
     * @return 시스템 기본 타임존 기반 Clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
// 이 클래스의 역할: 도메인 무관 인프라 빈의 등록 지점.
// SecurityConfig와 분리해 두면 순환 참조를 회피할 수 있고, 향후 공용 빈을 한곳에 모아 관리하기 쉬워진다.
