package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 회원가입 시 이미 사용 중인 이메일이 다시 입력된 경우.
 *
 * 409 CONFLICT를 사용해 "리소스 충돌"임을 의미적으로 표현한다.
 * 평문 비교 후 충돌을 알린다고 해서 사용자 enumeration이 곧장 가능해지진
 * 않는다 (login에서는 INVALID_CREDENTIALS로 통일). 회원가입 단계는 명시적
 * 안내가 UX상 이득이 더 크다.
 */
public class EmailAlreadyExistsException extends BusinessException {

    private static final String CODE = "EMAIL_ALREADY_EXISTS";
    private static final String MESSAGE = "이미 가입된 이메일이에요.";

    public EmailAlreadyExistsException() {
        super(CODE, MESSAGE, HttpStatus.CONFLICT);
    }
}
// 이 클래스의 역할: 이메일 중복 가입 시도를 표현하는 예외 (409 CONFLICT).
