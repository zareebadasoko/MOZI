package com.mozi.backend.domain.region.exception;

import com.mozi.backend.global.exception.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * 사용자 프로필 PUT 시 REGION 마스터에 존재하지 않는 sidoCode/sigunguCode 입력에 대한 예외.
 *
 * 발생 케이스:
 *  - sidoCode가 REGION 테이블에 없음
 *  - sigunguCode가 REGION 테이블에 없음
 *  - sidoCode 없이 sigunguCode만 입력 (시도 컨텍스트 누락)
 *
 * 행정구역에 FK를 걸지 않는 정책(§2-2)으로 인해 애플리케이션 레벨에서 검증하며,
 * 검증 실패는 클라이언트 입력 오류이므로 400 Bad Request로 응답한다.
 * errorCode "INVALID_REGION_CODE"로 프론트가 필드 단위 에러 표시에 활용.
 */
public class InvalidRegionException extends BusinessException {

    private static final String CODE = "INVALID_REGION_CODE";
    private static final String DEFAULT_MESSAGE = "행정구역 코드가 올바르지 않아요.";

    public InvalidRegionException() {
        super(CODE, DEFAULT_MESSAGE, HttpStatus.BAD_REQUEST);
    }

    public InvalidRegionException(String message) {
        super(CODE, message, HttpStatus.BAD_REQUEST);
    }
}
// 이 클래스의 역할: REGION 코드 검증 실패의 도메인 예외.
// FK 미설정 정책 하에서 앱 레벨 검증이 유일한 안전망 — 모든 진입점(PUT /api/users/me/profile)에서 호출.
