package com.mozi.backend.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 모든 REST API의 통일된 응답 래퍼.
 *
 * 성공/실패 양쪽 모두 동일한 JSON 구조로 반환하기 위해 단일 타입으로 둔다.
 * Validation 실패는 fields 객체로 필드별 에러를 분리해, 프론트가 각 입력란
 * 옆에 에러를 표시할 수 있게 한다. null 필드는 @JsonInclude(NON_NULL)로
 * 응답에서 자동 제외되어 success/error 응답이 각각 깔끔한 형태로 나간다.
 *
 * @param <T> data 페이로드 타입 (성공 응답에서만 의미 있음)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final String status;
    private final String message;
    private final T data;
    private final String errorCode;
    private final Map<String, String> fields;
    private final LocalDateTime timestamp;

    private ApiResponse(String status, String message, T data, String errorCode,
                        Map<String, String> fields, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.errorCode = errorCode;
        this.fields = fields;
        this.timestamp = timestamp;
    }

    /**
     * 메시지 없이 데이터만 담은 성공 응답.
     *
     * 별도의 안내 메시지가 필요 없는 단순 조회 API에서 사용.
     *
     * @param data 응답 페이로드 (null이면 응답에서 제외)
     * @param <T> 페이로드 타입
     * @return SUCCESS 상태의 ApiResponse
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", null, data, null, null, null);
    }

    /**
     * 메시지와 데이터를 모두 담은 성공 응답.
     *
     * 사용자에게 안내 문구가 필요한 경우 (예: "비밀번호가 변경되었어요") 사용.
     *
     * @param message 사용자 친화 한글 메시지
     * @param data 응답 페이로드 (null이면 응답에서 제외)
     * @param <T> 페이로드 타입
     * @return SUCCESS 상태의 ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data, null, null, null);
    }

    /**
     * 비즈니스 에러 응답.
     *
     * GlobalExceptionHandler가 BusinessException을 잡아 변환할 때 호출.
     * timestamp는 호출 시점에 자동 기록되어 디버깅 시 시간 추적 가능.
     *
     * @param errorCode 도메인별 에러 식별자 (예: "WELFARE_NOT_FOUND")
     * @param message 사용자 친화 한글 메시지
     * @return ERROR 상태의 ApiResponse (data 없음, timestamp 포함)
     */
    public static ApiResponse<Void> error(String errorCode, String message) {
        return new ApiResponse<>("ERROR", message, null, errorCode, null, LocalDateTime.now());
    }

    /**
     * Validation 실패 전용 에러 응답. errorCode는 항상 VALIDATION_FAILED.
     *
     * 프론트가 각 입력란 옆에 에러 표시를 할 수 있도록 fields 맵을 분리.
     *
     * @param message 사용자 친화 한글 메시지 (전체 폼 단위 안내)
     * @param fields 필드명 → 해당 필드 에러 메시지 맵
     * @return ERROR 상태의 ApiResponse (errorCode=VALIDATION_FAILED, fields 포함)
     */
    public static ApiResponse<Void> validationError(String message, Map<String, String> fields) {
        return new ApiResponse<>("ERROR", message, null, "VALIDATION_FAILED", fields, LocalDateTime.now());
    }
}
// 이 클래스의 역할: 모든 컨트롤러가 같은 응답 형식을 쓰도록 강제하는 단일 응답 래퍼.
// 컨트롤러는 정적 팩토리(success/error/validationError)로만 응답을 만들고,
// 직접 new로 생성할 수 없게 private 생성자로 막아 일관성을 유지한다.
// JsonInclude(NON_NULL) 덕에 success 응답에는 errorCode/timestamp/fields가 자동 제외된다.
