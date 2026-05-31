package com.mozi.backend.global.security;

import com.mozi.backend.domain.user.entity.Role;
import com.mozi.backend.global.exception.InvalidTokenException;
import com.mozi.backend.global.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트.
 *
 * Clock을 Clock.fixed로 주입해 발급 시점과 검증 시점을 결정적으로 통제한다.
 * 만료 직후로 시계를 옮긴 검증, 변조된 서명, 잘못된 형식 등 핵심 분기를 모두 검증.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "test-only-32+character-random-secret-for-hs256";
    private static final long ACCESS_TTL = 3600L;
    private static final long REFRESH_TTL = 604800L;

    private JwtTokenProvider provider(Clock clock) {
        JwtProperties props = new JwtProperties(SECRET, ACCESS_TTL, REFRESH_TTL);
        return new JwtTokenProvider(props, clock);
    }

    /**
     * 발급한 access token을 같은 시점 clock으로 파싱하면 sub/role 클레임이 복원된다.
     */
    @Test
    void generateAccessToken_파싱시_sub와role복원() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        JwtTokenProvider tp = provider(clock);

        String token = tp.generateAccessToken(42L, Role.USER);
        Claims claims = tp.parseAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    /**
     * 발급 후 1h(3600s) 직후에 검증하면 TokenExpiredException이 발생한다.
     */
    @Test
    void parseAccessToken_만료후_TokenExpiredException발생() {
        Clock issueClock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        String token = provider(issueClock).generateAccessToken(1L, Role.USER);

        // 1h 1초 뒤로 시계 이동 후 검증
        Clock verifyClock = Clock.fixed(Instant.parse("2026-05-10T11:00:01Z"), ZoneOffset.UTC);
        JwtTokenProvider verifier = provider(verifyClock);

        assertThatThrownBy(() -> verifier.parseAccessToken(token))
                .isInstanceOf(TokenExpiredException.class);
    }

    /**
     * 서명을 변조한 토큰은 InvalidTokenException으로 변환된다.
     */
    @Test
    void parseAccessToken_서명변조_InvalidTokenException발생() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        JwtTokenProvider tp = provider(clock);
        String token = tp.generateAccessToken(1L, Role.USER);
        // 마지막 1글자 변경 → 서명 불일치
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> tp.parseAccessToken(tampered))
                .isInstanceOf(InvalidTokenException.class);
    }

    /**
     * 형식이 아예 JWT가 아닌 문자열도 InvalidTokenException으로 변환된다.
     */
    @Test
    void parseAccessToken_형식오류_InvalidTokenException발생() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);
        JwtTokenProvider tp = provider(clock);

        assertThatThrownBy(() -> tp.parseAccessToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    /**
     * generateRefreshTokenRaw는 매번 다른 값(엔트로피 충분)을 반환한다.
     */
    @Test
    void generateRefreshTokenRaw_매호출_다른값반환() {
        JwtTokenProvider tp = provider(Clock.systemDefaultZone());
        String a = tp.generateRefreshTokenRaw();
        String b = tp.generateRefreshTokenRaw();

        assertThat(a).isNotEqualTo(b);
        // Base64URL no-padding이라 = 문자가 없어야 함
        assertThat(a).doesNotContain("=");
        assertThat(a.length()).isGreaterThan(40);
    }

    /**
     * hashRefreshToken은 SHA-256 hex 64자를 반환하며 결정적이다.
     */
    @Test
    void hashRefreshToken_SHA256_hex_64자_결정적() {
        JwtTokenProvider tp = provider(Clock.systemDefaultZone());
        String raw = "any-raw-string";

        String h1 = tp.hashRefreshToken(raw);
        String h2 = tp.hashRefreshToken(raw);

        assertThat(h1).hasSize(64);
        assertThat(h1).matches("[0-9a-f]+");
        assertThat(h1).isEqualTo(h2);
    }

    /**
     * TTL getter가 properties 값을 그대로 노출한다.
     */
    @Test
    void TTL_getter_properties값과일치() {
        JwtTokenProvider tp = provider(Clock.systemDefaultZone());
        assertThat(tp.getAccessTtlSeconds()).isEqualTo(ACCESS_TTL);
        assertThat(tp.getRefreshTtlSeconds()).isEqualTo(REFRESH_TTL);
    }
}
// 이 테스트의 역할: 토큰 cryptographic 책임의 모든 분기를 결정적으로 검증.
// Clock.fixed 주입으로 만료 직전/직후 분기를 시계 의존성 없이 단정할 수 있다.
