package com.mozi.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * Access JWT의 형식·서명이 잘못된 경우 던지는 예외.
 *
 * jjwt가 던지는 JwtException / IllegalArgumentException 계열을 묶어
 * 도메인 의미가 명확한 예외로 변환한다. "토큰이 있지만 신뢰할 수 없음"을
 * 의미하므로 만료(TokenExpiredException)와는 별도 errorCode로 구분된다.
 */
public class InvalidTokenException extends BusinessException {

    private static final String CODE = "INVALID_TOKEN";
    private static final String MESSAGE = "유효하지 않은 인증 정보예요.";

    public InvalidTokenException() {
        super(CODE, MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
// 이 클래스의 역할: 변조·형식 오류 토큰을 표현하는 예외 (TOKEN_EXPIRED와 구분).
