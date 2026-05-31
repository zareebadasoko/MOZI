package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 비밀번호 변경 시 현재 비밀번호 검증에 실패한 경우.
 *
 * 로그인 단계가 아닌 인증된 상태에서의 재확인 단계라 INVALID_CREDENTIALS와
 * 별도 errorCode를 사용해 클라이언트가 폼 위치를 정확히 안내할 수 있게 한다.
 */
public class PasswordMismatchException extends BusinessException {

    private static final String CODE = "PASSWORD_MISMATCH";
    private static final String MESSAGE = "현재 비밀번호가 일치하지 않아요.";

    public PasswordMismatchException() {
        super(CODE, MESSAGE, HttpStatus.BAD_REQUEST);
    }
}
// 이 클래스의 역할: 비밀번호 변경 시 현재 비밀번호 불일치를 표현.
// 로그인 실패(INVALID_CREDENTIALS)와는 컨텍스트가 달라 분리해 둔다.
