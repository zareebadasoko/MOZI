package com.mozi.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * Access JWT의 exp 클레임이 현재 시각보다 이전이라 만료된 경우.
 *
 * 클라이언트는 이 errorCode를 보고 /api/auth/refresh 엔드포인트로 이동해
 * 새 토큰을 발급받도록 동작해야 한다. INVALID_TOKEN과 구분되는 별도 코드인 이유가 바로 그 클라이언트 분기 때문.
 */
public class TokenExpiredException extends BusinessException {

    private static final String CODE = "TOKEN_EXPIRED";
    private static final String MESSAGE = "로그인 세션이 만료되었어요. 다시 로그인해주세요.";

    public TokenExpiredException() {
        super(CODE, MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
// 이 클래스의 역할: 만료된 access token을 명시적으로 구분하는 예외.
// 클라이언트는 이 코드를 보고 refresh API 호출로 자동 분기한다.
