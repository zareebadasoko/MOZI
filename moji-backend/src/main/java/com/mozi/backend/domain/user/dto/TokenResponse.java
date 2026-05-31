package com.mozi.backend.domain.user.dto;

/**
 * 로그인/리프레시 응답 DTO.
 *
 * accessToken은 매 보호 API 호출 시 Authorization: Bearer 헤더에 사용,
 * refreshToken은 access가 만료되었을 때 새 토큰 쌍을 받기 위해 사용한다.
 * tokenType은 항상 "Bearer" 고정이지만 클라이언트가 헤더 prefix를 결정할 때
 * 참고할 수 있도록 응답에 포함한다. expiresIn은 access 토큰의 TTL(초).
 *
 * @param accessToken 서명된 JWT
 * @param refreshToken raw refresh 토큰 (1회 노출, 서버에는 해시만 저장)
 * @param tokenType 항상 "Bearer"
 * @param expiresIn access 토큰 유효 기간 (초)
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
// 이 record의 역할: 로그인·리프레시 응답 페이로드.
// expiresIn 단위가 ms가 아닌 초인 이유는 OAuth2 표준(RFC 6749) 관례를 따르기 위함.
