package com.mozi.backend.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증 정보가 아예 제공되지 않았을 때 던지는 예외.
 *
 * Authorization 헤더 누락 같은 상황에서 사용한다.
 * AuthenticationEntryPoint가 직접 응답을 쓰는 경로 외에, 컨트롤러에서
 * 명시적으로 거절해야 하는 경우(예: principal이 null인 logout)에도 사용.
 */
public class UnauthorizedException extends BusinessException {

    private static final String CODE = "UNAUTHORIZED";
    private static final String MESSAGE = "로그인이 필요해요.";

    public UnauthorizedException() {
        super(CODE, MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
// 이 클래스의 역할: 미인증 상태를 명시적으로 표현하는 예외.
