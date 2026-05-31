package com.mozi.backend.domain.welfare.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * GET /api/welfares/{id} 호출 시 해당 ID의 복지 row가 존재하지 않을 때.
 *
 * 자연키(VARCHAR 12)라 클라이언트가 임의 문자열을 넣어 호출할 가능성이 있어
 * 명시적 404 + WELFARE_NOT_FOUND 응답으로 안내한다. 동일 ID가 자식 4종 어느
 * 테이블에도 없을 때만 발생.
 */
public class WelfareNotFoundException extends BusinessException {

    private static final String CODE = "WELFARE_NOT_FOUND";
    private static final String MESSAGE = "복지 정보를 찾을 수 없어요.";

    public WelfareNotFoundException() {
        super(CODE, MESSAGE, HttpStatus.NOT_FOUND);
    }
}
// 이 클래스의 역할: 존재하지 않는 welfareId 호출에 대한 표준 404 응답.
