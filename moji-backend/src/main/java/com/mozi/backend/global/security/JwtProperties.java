package com.mozi.backend.global.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT 발급/검증에 사용되는 설정값 모음.
 *
 * application.yml 의 mozi.jwt.* 키에 바인딩되며 @Validated 로 부팅 시점에
 * 비어 있거나 너무 짧은 secret을 즉시 검출한다. 이렇게 하면 dev 환경에서
 * 잘못된 설정으로 컨텍스트가 일단 떠버리는 것을 방지할 수 있다.
 *
 * @param secret HS256 서명용 비밀키. JWT 라이브러리가 요구하는 최소 256-bit
 *               (= 32 바이트, 영문 32자) 이상이어야 한다.
 * @param accessTokenExpirySeconds Access JWT 만료 (기본 3600s = 1h)
 * @param refreshTokenExpirySeconds Refresh 토큰 만료 (기본 604800s = 7d)
 */
@Validated
@ConfigurationProperties(prefix = "mozi.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Positive long accessTokenExpirySeconds,
        @Positive long refreshTokenExpirySeconds
) {
}
// 이 record의 역할: JWT 설정값을 타입 안전하게 한 곳에 모은다.
// secret 길이는 HS256 알고리즘 요구사항(256-bit 이상)을 강제하기 위해 32자 이상으로 검증한다.
