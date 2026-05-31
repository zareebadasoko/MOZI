package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 로그인 시 이메일/비밀번호 조합이 일치하지 않는 경우.
 *
 * 이메일이 존재하지 않을 때와 비밀번호가 틀릴 때 모두 같은 errorCode로 응답한다.
 * 이렇게 하면 "이 이메일은 가입된 적 없습니다" 같은 메시지로 인한
 * 사용자 enumeration 누설을 막을 수 있다.
 */
public class InvalidCredentialsException extends BusinessException {

    private static final String CODE = "INVALID_CREDENTIALS";
    private static final String MESSAGE = "이메일 또는 비밀번호가 올바르지 않아요.";

    public InvalidCredentialsException() {
        super(CODE, MESSAGE, HttpStatus.UNAUTHORIZED);
    }
}
// 이 클래스의 역할: 로그인 실패를 일관 메시지로 표현해 enumeration 공격을 방지한다.
