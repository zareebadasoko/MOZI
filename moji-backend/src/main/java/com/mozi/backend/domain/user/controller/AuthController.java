package com.mozi.backend.domain.user.controller;

import com.mozi.backend.domain.user.dto.LoginRequest;
import com.mozi.backend.domain.user.dto.LogoutResponse;
import com.mozi.backend.domain.user.dto.RefreshRequest;
import com.mozi.backend.domain.user.dto.SignupRequest;
import com.mozi.backend.domain.user.dto.SignupResponse;
import com.mozi.backend.domain.user.dto.TokenResponse;
import com.mozi.backend.domain.user.service.AuthService;
import com.mozi.backend.global.common.ApiResponse;
import com.mozi.backend.global.exception.UnauthorizedException;
import com.mozi.backend.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 흐름 4개 엔드포인트의 진입점.
 *
 * 모두 POST 메서드를 사용하며 응답은 ApiResponse 래퍼로 통일한다.
 * signup/login/refresh는 SecurityConfig의 permitAll()로 인증 없이 접근 가능,
 * logout만 보호 라우트라 SecurityContext의 principal이 채워진 상태로 호출된다.
 */
@Tag(name = "Auth", description = "회원가입 / 로그인 / 토큰 갱신 / 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입.
     *
     * 가입 직후 즉시 사용 가능한 토큰까지 함께 반환해 클라이언트가 별도 로그인
     * 호출 없이 바로 메인 화면 진입 가능하게 한다.
     *
     * @param request 이메일 + 비밀번호
     * @return userId + 토큰 1쌍
     */
    @Operation(summary = "회원가입", description = "이메일·비밀번호로 가입하고 즉시 access/refresh 토큰을 발급받는다.")
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("회원가입이 완료되었어요.", authService.signup(request));
    }

    /**
     * 로그인.
     *
     * 자격증명 검증 통과 시 새 토큰 1쌍을 발급한다. 다중 기기 공존 정책이라
     * 다른 기기의 RefreshToken은 그대로 유지된다.
     *
     * @param request 이메일 + 비밀번호
     * @return 새 토큰 1쌍
     */
    @Operation(summary = "로그인", description = "이메일·비밀번호 검증 후 새 access/refresh 토큰을 발급한다. 다중 기기 공존 정책.")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * Refresh Token Rotation.
     *
     * 기존 토큰 row 삭제 + 새 토큰 1쌍 발급 + 새 row 저장이 한 트랜잭션에서
     * 일어난다. 같은 raw 토큰으로 재호출 시 두 번째 호출은 INVALID_REFRESH_TOKEN.
     *
     * @param request raw refresh 토큰
     * @return 새 토큰 1쌍
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token Rotation — 기존 토큰 무효화 후 새 1쌍 발급. 같은 raw 토큰으로 재호출 시 INVALID_REFRESH_TOKEN.")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    /**
     * 로그아웃 — 다중 기기 강제 종료.
     *
     * @param principal SecurityContext에 담긴 인증 사용자. 보호 라우트라 정상 흐름에선 null이 아님.
     * @return loggedOut=true
     * @throws UnauthorizedException principal이 null인 비정상 케이스 (SecurityFilter 우회)
     */
    @Operation(summary = "로그아웃 (전체 기기)", description = "현재 사용자의 모든 RefreshToken row를 삭제 — 다중 기기 일괄 종료.")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (principal == null) {
            // 정상적으로는 SecurityConfig의 .anyRequest().authenticated() 단계에서
            // 401이 먼저 응답되므로 도달할 일 없음 — 안전망으로만 둔다.
            throw new UnauthorizedException();
        }
        return ApiResponse.success("로그아웃되었어요.", authService.logout(principal.userId()));
    }
}
// 이 클래스의 역할: HTTP ↔ AuthService 어댑터.
// 비즈니스 로직은 모두 AuthService가 담당하며 컨트롤러는 입력 검증과 응답 래핑만 한다.
