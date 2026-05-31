package com.mozi.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

/**
 * 프론트엔드(다른 Origin)에서 본 백엔드 API를 호출할 수 있도록 CORS 정책을 정의한다.
 *
 * 본 빈은 SecurityConfig의 .cors() 단계에서 자동 적용된다 — Spring Security가
 * CorsConfigurationSource 타입 빈을 컨텍스트에서 찾아 사용한다.
 *
 * 허용 정책:
 *  - Origin: localhost:5173 (Vite 기본 포트), localhost:3000 (CRA·Next 관습 포트)
 *  - Method: GET / POST / PUT / DELETE / OPTIONS (PATCH는 현재 미사용)
 *  - Header: Authorization (JWT) / Content-Type (JSON)
 *  - Credentials: false (현 인증은 헤더 토큰 방식이라 쿠키 자격증명 불필요)
 *
 * 운영 배포 시 Origin 목록을 환경 변수로 외부화하거나 프로파일별 분기를 추가해야 한다.
 */
@Configuration
public class CorsConfig {

    /**
     * Spring Security와 통합되는 CORS 설정 빈.
     *
     * `/api/**` 경로에만 정책을 적용한다 — 도메인 외 경로(루트 등)는 영향 X.
     *
     * @return URL 기반 CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 졸업 프로젝트용 두 포트만 명시 허용 — 와일드카드 금지 (보안 + allowCredentials 조합 호환성)
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        // preflight 응답 캐시 — 동일 요청 반복 시 OPTIONS 호출 줄여 응답성 향상
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
// 이 클래스의 역할: 로컬 개발용 프론트엔드(5173/3000)에서 백엔드 API를 호출 가능하게 하는 CORS 정책 단일 정의 지점.
// 새 Origin 추가가 필요해지면 setAllowedOrigins 목록에 한 줄 추가하면 된다.
