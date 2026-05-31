package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 비밀번호 변경 시 새 비밀번호가 현재 비밀번호와 동일한 경우.
 *
 * BCrypt matches 1회로 비교 가능. 사용자가 비밀번호를 그대로 다시 입력해
 * 변경 효과가 없는 상황을 명시적으로 안내한다.
 */
public class SamePasswordException extends BusinessException {

    private static final String CODE = "SAME_PASSWORD";
    private static final String MESSAGE = "새 비밀번호가 현재 비밀번호와 같아요.";

    public SamePasswordException() {
        super(CODE, MESSAGE, HttpStatus.BAD_REQUEST);
    }
}
// 이 클래스의 역할: 새 비밀번호 = 현재 비밀번호 케이스 사용자 안내.
