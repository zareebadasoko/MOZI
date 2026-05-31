package com.mozi.backend.domain.user.dto;

/**
 * 회원가입 응답 DTO.
 *
 * TokenResponse와 동일한 토큰 정보에 userId가 추가된 형태. 클라이언트는
 * 가입 직후 즉시 로그인된 상태로 진입할 수 있도록 토큰을 함께 받는다.
 * userId는 가입 후 첫 프로필 PUT(Phase 4) 호출의 path/body에 활용 가능.
 *
 * @param userId 발급된 사용자 PK
 * @param accessToken 서명된 JWT
 * @param refreshToken raw refresh 토큰
 * @param tokenType 항상 "Bearer"
 * @param expiresIn access 토큰 TTL (초)
 */
public record SignupResponse(
        Long userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
// 이 record의 역할: 회원가입 응답 페이로드.
// TokenResponse와 합쳐서 상속 구조로 만들지 않고 평면 record로 둔 이유는
// API_SPEC.md 형식과 1:1 일치시키기 위함이다.
