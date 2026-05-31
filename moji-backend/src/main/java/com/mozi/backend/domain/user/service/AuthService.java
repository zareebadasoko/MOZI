package com.mozi.backend.domain.user.service;

import com.mozi.backend.domain.user.dto.LoginRequest;
import com.mozi.backend.domain.user.dto.LogoutResponse;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.dto.SignupResponse;
import com.mozi.backend.domain.user.dto.TokenResponse;
import com.mozi.backend.domain.user.entity.RefreshToken;
import com.mozi.backend.domain.user.entity.User;
import com.mozi.backend.domain.user.exception.EmailAlreadyExistsException;
import com.mozi.backend.domain.user.exception.InvalidCredentialsException;
import com.mozi.backend.domain.user.exception.InvalidRefreshTokenException;
import com.mozi.backend.domain.user.repository.RefreshTokenRepository;
import com.mozi.backend.domain.user.repository.UserRepository;
import com.mozi.backend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 인증 흐름(가입/로그인/리프레시/로그아웃)의 비즈니스 로직.
 *
 * 정책 요약:
 * (1) 회원가입 — 이메일 중복 차단 → BCrypt 해시 → User 생성 → 토큰 1쌍 발급
 * (2) 로그인 — 자격증명 검증 → 토큰 1쌍 발급. 다중 기기 공존이라 기존 RefreshToken 보존
 * (3) 리프레시 — Rotation: 기존 row 삭제 + 새 row 발급. 만료/누락은 모두 INVALID_REFRESH_TOKEN
 * (4) 로그아웃 — 사용자의 모든 RefreshToken 일괄 삭제 (다중 기기 강제 종료)
 *
 * Clock은 SecurityConfig 빈으로부터 주입받아 단위 테스트에서 시간 결정성을 확보한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final Clock clock;

    /**
     * 회원가입 처리.
     *
     * 이메일 중복 시 EmailAlreadyExistsException(409). 정상 흐름에서는 User insert와
     * RefreshToken insert가 한 트랜잭션에 묶여 함께 커밋된다 — 가입은 됐는데 토큰이 없는
     * 중간 상태가 생기지 않게 하기 위함. UserProfile은 lazy creation 정책에 따라
     * 가입 시점엔 만들지 않는다 (Phase 4의 첫 프로필 PUT에서 생성).
     *
     * @param request 이메일/평문 비밀번호
     * @return userId + 토큰 1쌍
     * @throws EmailAlreadyExistsException 이메일이 이미 가입된 경우
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException();
        }
        // 평문 비밀번호 저장 방지: BCrypt로 해싱한 결과만 영속화
        User user = User.of(request.email(), passwordEncoder.encode(request.password()));
        userRepository.save(user);

        TokenResponse tokens = issueTokensFor(user);
        return new SignupResponse(
                user.getId(),
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresIn()
        );
    }

    /**
     * 로그인 처리.
     *
     * 이메일이 없거나 비밀번호가 틀린 경우 모두 InvalidCredentialsException으로 통일 응답해
     * 사용자 enumeration 누설을 차단한다. 다중 기기 공존 정책상 기존 RefreshToken은
     * 삭제하지 않고 새 row만 추가 발급.
     *
     * @param request 이메일/평문 비밀번호
     * @return 새 토큰 1쌍
     * @throws InvalidCredentialsException 자격증명 불일치
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        // BCrypt matches 검증: 평문 ↔ 저장된 해시 비교
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return issueTokensFor(user);
    }

    /**
     * Refresh Token Rotation 처리.
     *
     * 알고리즘:
     * 1) raw → SHA-256 해시 → DB row 조회. 없으면 INVALID_REFRESH_TOKEN
     * 2) 만료 row면 즉시 삭제 + INVALID_REFRESH_TOKEN
     * 3) 통과 시 기존 row 삭제(Rotation step 1) → 새 토큰 1쌍 발급(step 2)
     *
     * 한 raw token을 두 번 사용하면 두 번째 호출은 1)에서 row가 없어 자동으로 차단된다 —
     * 토큰 탈취 후 도용 시도를 amount of effort 1로 무력화하는 것이 Rotation의 목적.
     *
     * @param rawRefreshToken 클라이언트가 보낸 raw refresh 토큰
     * @return 새 토큰 1쌍
     * @throws InvalidRefreshTokenException row 부재 / 만료 / 재사용 모두 동일 응답
     */
    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        String hash = tokenProvider.hashRefreshToken(rawRefreshToken);
        RefreshToken row = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (row.isExpired(LocalDateTime.now(clock))) {
            // 만료 row는 차후 정리할 필요 없도록 즉시 제거
            refreshTokenRepository.delete(row);
            throw new InvalidRefreshTokenException();
        }
        User user = row.getUser();
        // ROTATION: 기존 row 삭제 후 새 토큰 1쌍 발급. 동일 raw 재사용 시 다음 호출은 row 부재로 자동 차단.
        refreshTokenRepository.delete(row);
        return issueTokensFor(user);
    }

    /**
     * 로그아웃 — 사용자의 모든 RefreshToken row 일괄 삭제.
     *
     * 다중 기기 강제 종료 정책: 한 기기에서 logout을 호출하면 나머지 기기의
     * refresh도 모두 무효화된다. (Access 토큰은 짧은 TTL이라 자연 만료 대기)
     *
     * @param userId 인증된 사용자 PK
     * @return 항상 loggedOut=true
     */
    @Transactional
    public LogoutResponse logout(Long userId) {
        refreshTokenRepository.deleteAllByUser_Id(userId);
        return new LogoutResponse(true);
    }

    /**
     * 사용자에게 access JWT + refresh raw 토큰 1쌍을 발급하고 RefreshToken row를 저장한다.
     *
     * 발급 시점의 raw 값을 클라이언트에 1회 노출하고 서버에는 SHA-256 hex만 저장한다.
     * expiresAt은 (clock 기준 현재 시각 + refresh TTL).
     *
     * @param user 영속화된 사용자 (PK 부여 완료)
     * @return TokenResponse (access/refresh raw + tokenType + expiresIn)
     */
    private TokenResponse issueTokensFor(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole());
        String rawRefresh = tokenProvider.generateRefreshTokenRaw();
        String hash = tokenProvider.hashRefreshToken(rawRefresh);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusSeconds(tokenProvider.getRefreshTtlSeconds());
        refreshTokenRepository.save(RefreshToken.issue(user, hash, expiresAt));
        return new TokenResponse(accessToken, rawRefresh, TOKEN_TYPE, tokenProvider.getAccessTtlSeconds());
    }
}
// 이 클래스의 역할: 인증 흐름의 트랜잭셔널 비즈니스 로직 단일 진입점.
// Rotation의 핵심은 refresh() 안의 "delete row → issueTokensFor()" 순서 — 순서가 바뀌면
// UNIQUE 제약(token_hash)에 걸리지 않더라도 이전 row가 살아 남아 재사용 차단이 깨진다.
