package com.mozi.backend.global.exception;

import com.mozi.backend.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * 모든 컨트롤러에서 던져진 예외를 가로채 통일된 ApiResponse로 변환하는 전역 핸들러.
 *
 * 개별 컨트롤러가 try-catch를 남발하지 않도록 @RestControllerAdvice로 전역 처리.
 * 비즈니스 예외 / Validation 실패 / 그 외 예상 못한 예외 3가지 케이스를 분기 처리한다.
 * 핸들러는 "더 구체적인 타입"이 우선 매칭되므로 Exception 핸들러는 fallback으로 동작.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인 비즈니스 예외 처리.
     *
     * 예외에 담긴 errorCode/message/httpStatus를 그대로 응답으로 변환한다.
     * BusinessException을 상속한 모든 도메인 예외(UserNotFound 등)도 이 핸들러가 처리.
     *
     * @param e BusinessException 또는 그 자식 예외
     * @return errorCode 포함된 에러 ApiResponse + 예외에 명시된 HTTP 상태
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {} - {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity.status(e.getHttpStatus())
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    /**
     * Bean Validation(@Valid)에 의한 입력값 검증 실패 처리.
     *
     * 필드별 에러 메시지를 fields 맵으로 분리해 프론트가 각 입력란 옆에
     * 에러를 표시할 수 있도록 한다. 항상 400 + VALIDATION_FAILED 코드로 통일.
     *
     * @param e Bean Validation 실패 예외 (Spring이 @Valid 검증 실패 시 자동 발생)
     * @return fields 객체 포함 ApiResponse + HTTP 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> fields = new HashMap<>();
        // 필드별 에러를 맵으로 모아 프론트가 입력란 단위로 메시지를 표시할 수 있게 함
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            fields.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError("입력값을 확인해주세요.", fields));
    }

    /**
     * 필수 @RequestParam 누락 처리.
     *
     * 예: GET /api/categories에서 type 파라미터 미지정 시 발생. 500이 아닌 400으로
     * 응답하고 fields 맵에 누락된 파라미터 이름을 담아 클라이언트가 어떤 입력이 비어 있는지
     * 즉시 인지할 수 있게 한다.
     *
     * @param e Spring이 생성한 누락 예외
     * @return ApiResponse.validationError + HTTP 400
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        Map<String, String> fields = new HashMap<>();
        fields.put(e.getParameterName(), "필수 파라미터예요.");
        log.warn("Missing required param: {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError("필수 입력값이 빠졌어요.", fields));
    }

    /**
     * @RequestParam의 enum 값 매핑 실패 처리.
     *
     * 예: GET /api/categories?type=WRONG 같은 잘못된 enum 값. 500이 아닌 400으로
     * 응답해 클라이언트가 잘못된 입력을 알 수 있게 한다.
     *
     * @param e Spring이 생성한 매핑 실패 예외
     * @return ApiResponse.validationError + HTTP 400
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        Map<String, String> fields = new HashMap<>();
        fields.put(e.getName(), "허용되지 않는 값이에요.");
        log.warn("Type mismatch on param '{}': value={}", e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.validationError("입력값을 확인해주세요.", fields));
    }

    /**
     * 위 핸들러들로 잡히지 않은 모든 예외의 fallback.
     *
     * 보안상 사용자에게는 친화적 한글 메시지만 보여주고, 실제 스택 트레이스는
     * 서버 로그에만 기록한다 (내부 구조/SQL/스택 노출 방지).
     *
     * @param e 처리되지 않은 모든 Exception (NullPointerException, DB 예외 등)
     * @return INTERNAL_ERROR ApiResponse + HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR",
                        "잠시 후 다시 시도해주세요. 문제가 계속되면 고객센터로 문의해주세요."));
    }
}
// 이 클래스의 역할: 컨트롤러 전체의 예외를 한 곳에서 잡아 통일된 응답으로 변환.
// 핸들러 매칭 순서는 코드 순서가 아니라 "더 구체적인 타입이 먼저"라는 Spring 규칙에 따름.
// 새 예외 타입을 추가할 때는 BusinessException을 상속한 새 예외를 만들어 던지기만 하면 됨.
