package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * /api/auth/refresh 호출 시 토큰이 DB에 없거나 만료된 경우.
 *
 * Rotation 정책상 한 번 사용된 raw token은 두 번째 호출에서 row가 없어
 * 자동으로 이 예외를 발생시킨다. 즉 "재사용 시도"도 이 예외로 표현된다.
 * 클라이언트는 이 코드를 보면 refresh를 포기하고 재로그인 화면으로 이동해야 한다.
 */
public class InvalidRefreshTokenException extends BusinessException {

    private static final String CODE = "INVALID_REFRESH_TOKEN";
    private static final String MESSAGE = "재로그인이 필요해요.";

    public InvalidRefreshTokenException() {
        super(CODE, MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
// 이 클래스의 역할: refresh 단계의 모든 실패(없음/만료/재사용)를 동일 코드로 표현.
// 401 + INVALID_REFRESH_TOKEN 코드를 받으면 클라이언트는 재로그인 화면으로 이동하도록 설계.
