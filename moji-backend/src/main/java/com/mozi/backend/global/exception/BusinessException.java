package com.mozi.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반 시 던지는 예외의 베이스 클래스.
 *
 * 도메인별 예외(예: UserNotFoundException, WelfareNotFoundException)는
 * 이 클래스를 상속해 자체 errorCode/메시지/HttpStatus를 갖는다.
 * GlobalExceptionHandler가 이 타입(과 그 자식)을 한꺼번에 잡아
 * 통일된 ApiResponse로 변환해주므로, 컨트롤러/서비스에서는 try-catch를
 * 남발하지 않고 단순히 throw new XxxException()만 하면 된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    /**
     * @param errorCode 도메인별 에러 식별자 (예: "WELFARE_NOT_FOUND")
     * @param message 사용자 친화 한글 메시지 (그대로 응답에 노출됨)
     * @param httpStatus 응답에 사용할 HTTP 상태 코드 (예: HttpStatus.NOT_FOUND)
     */
    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
// 이 클래스의 역할: 모든 비즈니스 예외의 공통 부모.
// 도메인 예외 작성 시 이 클래스를 상속해 super(...)로 errorCode/message/httpStatus 지정.
// RuntimeException을 상속하므로 throws 선언 없이 던질 수 있어 컨트롤러/서비스가 깔끔.
