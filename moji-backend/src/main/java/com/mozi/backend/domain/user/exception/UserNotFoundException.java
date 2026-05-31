package com.mozi.backend.domain.user.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * SecurityContext에는 userId가 있는데 DB에 해당 사용자가 없는 모순 상황의 안전망.
 *
 * 정상 흐름에선 도달 불가능하지만, 회원 탈퇴 후 만료 직전 access token으로
 * 들어온 요청 같은 race condition 디버깅을 위해 명시적 예외로 분리한다.
 * UNAUTHORIZED와 다른 errorCode를 써서 로그에서 즉시 원인 파악이 가능하게 함.
 */
public class UserNotFoundException extends BusinessException {

    private static final String CODE = "USER_NOT_FOUND";
    private static final String MESSAGE = "사용자를 찾을 수 없어요.";

    public UserNotFoundException() {
        super(CODE, MESSAGE, HttpStatus.NOT_FOUND);
    }
}
// 이 클래스의 역할: 인증된 사용자 PK가 DB에 없는 모순 상황을 표현하는 안전망.
// 정상 흐름 도달 불가지만 디버깅 목적상 명시적 errorCode로 분리해 둔다.
