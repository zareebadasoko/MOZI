package com.mozi.backend.global.security;

import com.mozi.backend.domain.user.entity.Role;
import com.mozi.backend.global.exception.InvalidTokenException;
import com.mozi.backend.global.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

/**
 * JWT(Access)와 Refresh 토큰의 발급/파싱/해시 책임을 모은 컴포넌트.
 *
 * 분리된 책임:
 * (1) Access JWT — HS256 서명, sub=userId, role 클레임, exp = 발급 시각 + 1h
 * (2) Refresh raw — 32 바이트 SecureRandom + Base64URL no-padding (불투명 문자열)
 * (3) Refresh hash — SHA-256 hex 64자, DB 저장 전 일방향 해싱
 *
 * Refresh 토큰을 JWT로 만들지 않는 이유: Rotation을 위해 매 refresh마다 DB
 * lookup이 필수라 자기검증 토큰의 이점이 사라진다. opaque + 서버 측 row 검증이
 * 단순하고 안전.
 *
 * Clock을 주입받아 만료 검증을 결정적으로 만든다 — 단위 테스트에서 Clock.fixed로
 * 임의 시각을 강제할 수 있다.
 */
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        // HS256은 256-bit 이상 키를 요구. JwtProperties에서 32자 이상 강제됨.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = properties.accessTokenExpirySeconds();
        this.refreshTtlSeconds = properties.refreshTokenExpirySeconds();
        this.clock = clock;
    }

    /**
     * 사용자 식별자와 권한을 담은 Access JWT를 발급한다.
     *
     * sub 클레임에 userId 문자열, "role" 커스텀 클레임에 권한 enum 이름을 넣고
     * HS256으로 서명한다. exp는 (clock 기준 현재 시각 + accessTtlSeconds).
     *
     * @param userId 토큰에 담을 사용자 PK
     * @param role 사용자 권한 (USER/ADMIN)
     * @return 서명된 JWT 문자열
     */
    public String generateAccessToken(Long userId, Role role) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 32 바이트 무작위 → Base64URL no-padding 문자열 (~43자).
     *
     * 이 raw 값은 클라이언트에 1회 노출되고 서버는 SHA-256 해시 형태로만 저장한다.
     * 256-bit 엔트로피라 사전 공격이 사실상 불가능하므로 별도 saltedhash 없이 안전.
     *
     * @return 새 raw refresh 토큰 (Base64URL)
     */
    public String generateRefreshTokenRaw() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * raw refresh 토큰을 SHA-256 hex 64자로 변환.
     *
     * 발급 시 DB 저장 직전에, refresh 호출 시 들어온 raw를 lookup 키로 만들 때 호출된다.
     * BCrypt 대신 단방향 fast-hash를 쓰는 이유는 raw 자체가 256-bit 무작위라
     * 사전·brute-force 공격 모델이 적용되지 않기 때문이다.
     *
     * @param raw refresh 토큰 raw (Base64URL)
     * @return 64자 hex 해시
     */
    public String hashRefreshToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 부재는 표준 JRE라면 일어나지 않음 — 환경 자체가 이상한 케이스
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Access JWT를 파싱하고 서명/만료를 검증한다.
     *
     * 만료된 토큰은 TokenExpiredException, 서명·형식 오류는 InvalidTokenException으로
     * 변환해 호출자(필터)가 한 곳에서 분기 가능하게 한다.
     *
     * @param token 검증할 JWT
     * @return 검증 통과 시 클레임 객체 (sub, role, exp 등 포함)
     * @throws TokenExpiredException exp가 현재 시각보다 이전인 경우
     * @throws InvalidTokenException 서명 변조/형식 오류/null·빈 문자열 등
     */
    public Claims parseAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))   // 검증 시점도 주입된 clock 기준
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException();
        }
    }

    /**
     * Access TTL을 초 단위로 반환 — 응답의 expiresIn 필드 채울 때 사용.
     */
    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    /**
     * Refresh TTL을 초 단위로 반환 — 새 RefreshToken row의 expiresAt 계산에 사용.
     */
    public long getRefreshTtlSeconds() {
        return refreshTtlSeconds;
    }
}
// 이 클래스의 역할: 토큰 생성/검증의 cryptographic 디테일을 한 곳에 캡슐화.
// 서비스/필터는 이 클래스의 메서드만 호출하고 jjwt API에 직접 의존하지 않는다.
// Clock 주입으로 테스트에서 시간 결정성을 확보한다 — 만료 직전/직후 분기를 단위 검증 가능.
