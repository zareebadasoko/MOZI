package com.mozi.backend.domain.user.service;

import com.mozi.backend.domain.user.dto.LoginRequest;
import com.mozi.backend.domain.user.dto.LogoutResponse;
import com.mozi.backend.domain.user.dto.RefreshRequest;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.dto.SignupResponse;
import com.mozi.backend.domain.user.dto.TokenResponse;
import com.mozi.backend.domain.user.entity.RefreshToken;
import com.mozi.backend.domain.user.entity.Role;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.exception.EmailAlreadyExistsException;
import com.mozi.backend.domain.user.exception.InvalidCredentialsException;
import com.mozi.backend.domain.user.exception.InvalidRefreshTokenException;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.global.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthService Mockito 단위 테스트.
 *
 * 검증 대상:
 * - signup 중복 차단 / 정상 흐름
 * - login 자격증명 검증 + 다중 기기 공존(기존 row 보존)
 * - refresh Rotation: row 삭제 후 새 발급
 * - refresh 만료 row 처리
 * - refresh 미존재 raw 처리 (재사용 시도 포함)
 * - logout 일괄 삭제 호출
 *
 * Clock은 Clock.fixed로 주입해 만료 시각 계산을 결정적으로 만든다.
 * RefreshToken row의 PK는 IDENTITY 전략이라 save() 시점에 채워지지만 단위 테스트에서는
 * Mock이 무관하게 동작하므로 굳이 reflection으로 PK를 박지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-10T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        // @InjectMocks는 final 필드 Clock에 자동 주입을 못하므로 reflection으로 set
        ReflectionTestUtils.setField(authService, "clock", fixedClock);
    }

    /**
     * 이미 존재하는 이메일이면 EmailAlreadyExistsException(409) 발생 + User save 호출 X.
     */
    @Test
    void signup_중복이메일_예외발생() {
        when(userRepository.existsByEmail("dup@mozi.test")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(new SignupRequest("dup@mozi.test", "password1234")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    /**
     * 정상 signup: BCrypt 해싱 → User 저장 → 토큰 1쌍 발급 + RefreshToken row 저장.
     */
    @Test
    void signup_정상_토큰쌍반환() {
        when(userRepository.existsByEmail("new@mozi.test")).thenReturn(false);
        when(passwordEncoder.encode("password1234")).thenReturn("$2a$10$hash");
        // save() 시 PK 부여를 시뮬레이션
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 100L);
            return u;
        });
        when(tokenProvider.generateAccessToken(eq(100L), eq(Role.USER))).thenReturn("access-jwt");
        when(tokenProvider.generateRefreshTokenRaw()).thenReturn("raw-refresh");
        when(tokenProvider.hashRefreshToken("raw-refresh")).thenReturn("hashed");
        when(tokenProvider.getAccessTtlSeconds()).thenReturn(3600L);
        when(tokenProvider.getRefreshTtlSeconds()).thenReturn(604800L);

        SignupResponse res = authService.signup(new SignupRequest("new@mozi.test", "password1234"));

        assertThat(res.userId()).isEqualTo(100L);
        assertThat(res.accessToken()).isEqualTo("access-jwt");
        assertThat(res.refreshToken()).isEqualTo("raw-refresh");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresIn()).isEqualTo(3600L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    /**
     * 존재하지 않는 이메일 로그인 → INVALID_CREDENTIALS (404 아닌 401).
     */
    @Test
    void login_미존재이메일_INVALID_CREDENTIALS() {
        when(userRepository.findByEmail("nope@mozi.test")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nope@mozi.test", "password1234")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /**
     * 비밀번호 불일치 로그인 → INVALID_CREDENTIALS.
     */
    @Test
    void login_비밀번호불일치_INVALID_CREDENTIALS() {
        User user = User.of("ok@mozi.test", "$2a$10$stored");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("ok@mozi.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "$2a$10$stored")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ok@mozi.test", "wrong-pw")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    /**
     * 정상 로그인 — 기존 RefreshToken은 삭제하지 않고 새 row만 추가 (다중 기기 공존).
     */
    @Test
    void login_정상_기존_refresh_토큰_보존() {
        User user = User.of("ok@mozi.test", "$2a$10$stored");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findByEmail("ok@mozi.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1234", "$2a$10$stored")).thenReturn(true);
        when(tokenProvider.generateAccessToken(1L, Role.USER)).thenReturn("access-jwt");
        when(tokenProvider.generateRefreshTokenRaw()).thenReturn("new-raw");
        when(tokenProvider.hashRefreshToken("new-raw")).thenReturn("new-hash");
        when(tokenProvider.getAccessTtlSeconds()).thenReturn(3600L);
        when(tokenProvider.getRefreshTtlSeconds()).thenReturn(604800L);

        TokenResponse res = authService.login(new LoginRequest("ok@mozi.test", "password1234"));

        assertThat(res.refreshToken()).isEqualTo("new-raw");
        // 다중 기기 공존: 기존 토큰 일괄 삭제는 호출되지 않아야 함
        verify(refreshTokenRepository, never()).deleteAllByUser_Id(anyLong());
    }

    /**
     * Refresh Rotation 정상 흐름: 기존 row 삭제 후 새 발급.
     */
    @Test
    void refresh_정상_rotation_기존row삭제후_새발급() {
        User user = User.of("ok@mozi.test", "$2a$10$stored");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken existingRow = RefreshToken.issue(user, "old-hash",
                LocalDateTime.now(fixedClock).plusDays(7));

        when(tokenProvider.hashRefreshToken("raw-old")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(existingRow));
        when(tokenProvider.generateAccessToken(1L, Role.USER)).thenReturn("new-access");
        when(tokenProvider.generateRefreshTokenRaw()).thenReturn("raw-new");
        when(tokenProvider.hashRefreshToken("raw-new")).thenReturn("new-hash");
        when(tokenProvider.getAccessTtlSeconds()).thenReturn(3600L);
        when(tokenProvider.getRefreshTtlSeconds()).thenReturn(604800L);

        TokenResponse res = authService.refresh("raw-old");

        assertThat(res.refreshToken()).isEqualTo("raw-new");
        verify(refreshTokenRepository, times(1)).delete(existingRow);   // ROTATION 핵심: 기존 row 삭제
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    /**
     * Refresh raw가 DB에 없으면(재사용 시도 포함) INVALID_REFRESH_TOKEN.
     */
    @Test
    void refresh_미존재_raw_INVALID_REFRESH_TOKEN() {
        when(tokenProvider.hashRefreshToken("ghost")).thenReturn("ghost-hash");
        when(refreshTokenRepository.findByTokenHash("ghost-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("ghost"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    /**
     * Refresh row가 만료된 경우 즉시 삭제 + INVALID_REFRESH_TOKEN.
     */
    @Test
    void refresh_만료row_삭제후_INVALID_REFRESH_TOKEN() {
        User user = User.of("ok@mozi.test", "$2a$10$stored");
        ReflectionTestUtils.setField(user, "id", 1L);
        RefreshToken expiredRow = RefreshToken.issue(user, "expired-hash",
                LocalDateTime.now(fixedClock).minusSeconds(1));

        when(tokenProvider.hashRefreshToken("raw-expired")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredRow));

        assertThatThrownBy(() -> authService.refresh("raw-expired"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(refreshTokenRepository, times(1)).delete(expiredRow);   // 만료 row는 즉시 정리
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    /**
     * 로그아웃 — deleteAllByUser_Id가 정확히 1회 호출된다.
     */
    @Test
    void logout_정상_사용자의모든_refresh_token_삭제() {
        LogoutResponse res = authService.logout(42L);

        assertThat(res.loggedOut()).isTrue();
        verify(refreshTokenRepository, times(1)).deleteAllByUser_Id(42L);
    }
}
// 이 테스트의 역할: AuthService의 모든 분기를 외부 의존성 없이 검증.
// Clock.fixed 주입으로 expiresAt 계산이 결정적이라 만료 분기도 단정 가능.
