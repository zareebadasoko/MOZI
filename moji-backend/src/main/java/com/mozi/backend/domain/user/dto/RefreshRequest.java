package com.mozi.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token Rotation 요청 DTO.
 *
 * 클라이언트가 발급받았던 raw refresh 토큰(Base64URL 문자열)을 그대로 보낸다.
 * 서버는 이를 SHA-256 해시해 DB에서 매칭되는 row를 찾아 검증한다.
 *
 * @param refreshToken raw refresh 토큰 (Base64URL no-padding)
 */
public record RefreshRequest(
        @NotBlank(message = "refreshToken을 입력해주세요.")
        String refreshToken
) {
}
// 이 record의 역할: refresh API 본문 검증.
// 토큰 형식 검증은 서비스 레이어에서 SHA-256 lookup 결과로 자동 처리되므로
// 여기선 빈 문자열 차단만 한다.
