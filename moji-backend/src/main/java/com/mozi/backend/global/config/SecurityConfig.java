package com.mozi.backend.global.config;

import com.mozi.backend.global.security.JwtAccessDeniedHandler;
import com.mozi.backend.global.security.JwtAuthenticationEntryPoint;
import com.mozi.backend.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정 단일 진입점.
 *
 * 핵심 정책:
 * (1) Stateless — 세션 미사용. 매 요청마다 JwtAuthenticationFilter가 SecurityContext 재구성
 * (2) CSRF/CORS/formLogin/httpBasic/logout 모두 비활성 — REST + JWT 환경에서 불필요
 * (3) 공개 라우트: /api/auth/{signup,login,refresh}, /api/health
 * (4) 그 외 모든 라우트는 authenticated() — 토큰 없이 접근 시 EntryPoint가 401 응답
 * (5) 인증 실패는 JwtAuthenticationEntryPoint, 인가 실패는 JwtAccessDeniedHandler로 위임
 *
 * PasswordEncoder 빈만 여기서 정의한다. Clock 빈은 AppConfig에 분리되어 있다 —
 * SecurityConfig 안에 Clock을 두면 SecurityConfig → JwtAuthenticationFilter →
 * JwtTokenProvider → Clock(SecurityConfig) 순환 참조가 발생하기 때문.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * 비밀번호 해싱/검증용 BCryptPasswordEncoder 빈.
     *
     * 단일 빈으로 등록해 시드 로더(UserSeedLoader)·AuthService가 동일 인스턴스를 공유하게 한다.
     * 강도(strength)는 기본값 10 — BCrypt 표준이고 CPU 부담도 적당.
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정.
     *
     * 의존 빈(필터/EntryPoint/Handler)은 클래스 필드가 아닌 메서드 매개변수로 주입받는다.
     * 이렇게 하면 Spring이 lazy resolve를 쓸 수 있어 순환 참조 위험이 더 작아진다.
     *
     * @param http HttpSecurity DSL
     * @param jwtAuthenticationFilter Bearer 토큰 → SecurityContext 변환 필터
     * @param authenticationEntryPoint 인증 실패 시 401 JSON 응답기
     * @param accessDeniedHandler 인가 실패 시 403 JSON 응답기
     * @return 구성된 SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   JwtAuthenticationEntryPoint authenticationEntryPoint,
                                                   JwtAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Phase 6: CorsConfig가 등록한 CorsConfigurationSource 빈을 자동 사용 (localhost:5173, :3000 허용)
                .cors(cors -> {})
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh"
                        ).permitAll()
                        .requestMatchers("/api/health").permitAll()
                        // Phase 4-2: 복지 검색·상세·카테고리는 비로그인도 접근 가능 (Optional auth)
                        // 로그인 시 isBookmarked만 자동 반영, 비로그인 시엔 모두 false (Step 6에서 applyMyProfile 제거)
                        // Step 1 (USER_PROFILE_REDESIGN): /api/regions 신규 — Region 마스터 조회는 비로그인 접근
                        .requestMatchers("/api/welfares", "/api/welfares/**", "/api/categories", "/api/regions").permitAll()
                        // Phase 6: Springdoc OpenAPI + Swagger UI 경로 공개. 운영 환경에선 별도 차단 검토.
                        .requestMatchers(
                                "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui.html", "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
// 이 클래스의 역할: Stateless JWT 기반 보안 정책의 단일 정의 지점.
// 새 공개 라우트가 생기면 여기 authorizeHttpRequests 부분만 추가한다.
// 인증/인가 실패 응답 형식 변경이 필요할 땐 EntryPoint/AccessDeniedHandler를 수정한다.
