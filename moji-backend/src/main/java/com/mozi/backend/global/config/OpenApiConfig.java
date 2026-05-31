package com.mozi.backend.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI 메타 정보 설정.
 *
 * 본 클래스는 Swagger UI 상단에 표시되는 서비스명·설명·서버 URL과,
 * 인증 보호 엔드포인트에서 사용할 JWT Bearer 인증 스키마를 등록한다.
 *
 * 인증 사용법(Swagger UI 우상단 "Authorize" 버튼):
 *  1) 회원가입 또는 로그인 엔드포인트 호출 → access 토큰 수신
 *  2) "Authorize" 버튼 → "Bearer {token}" 입력
 *  3) 이후 인증 필요 엔드포인트가 자동으로 헤더 포함하여 호출됨
 *
 * 컨트롤러/엔드포인트별 추가 메타(@Tag, @Operation 등)는 각 컨트롤러 파일에서 선언한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * MOZI 백엔드의 OpenAPI 메타데이터 빈.
     *
     * Spring Boot 부팅 시 Springdoc이 본 빈을 자동 인식해 `/v3/api-docs`와 Swagger UI에 반영한다.
     *
     * @return 서비스 메타 + JWT 인증 스키마가 포함된 OpenAPI 인스턴스
     */
    @Bean
    public OpenAPI moziOpenAPI() {
        Info info = new Info()
                .title("MOZI 백엔드 API")
                .version("v1")
                .description("""
                        노인 맞춤형 통합 복지 안내 서비스의 백엔드 API.

                        - 인증 방식: JWT Bearer 토큰 (Authorization 헤더)
                        - 응답 포맷: ApiResponse 래퍼 (status / message / data 또는 errorCode)
                        - 페이지네이션: PageResponse (items / page / size / totalCount / hasNext)
                        - 자세한 사용법: docs/API_SPEC.md, docs/FRONTEND_INTEGRATION_GUIDE.md, docs/ERROR_CODES.md
                        """)
                .contact(new Contact().name("MOZI Backend").email("noreply@mozi.test"));

        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("로컬 개발 서버");

        // JWT Bearer 인증 스키마 정의 — Swagger UI "Authorize" 버튼과 연동
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // 글로벌 SecurityRequirement는 두지 않는다 — 각 컨트롤러/메서드에서 @SecurityRequirement(name="BearerAuth")로 명시.
        // 글로벌 요구사항을 두면 비인증 엔드포인트마다 @SecurityRequirements({})로 비워야 하는데,
        // 인자 없는 @SecurityRequirements 어노테이션이 Springdoc 일부 버전에서 NPE를 유발하는 케이스가 있어 회피.
        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme));
    }
}
// 이 클래스의 역할: Swagger UI에 노출되는 서비스 메타 + JWT 인증 스키마의 단일 정의 지점.
// 인증이 필요한 엔드포인트는 컨트롤러/메서드 단위로 @SecurityRequirement(name="BearerAuth") 명시.
