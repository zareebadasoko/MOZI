package com.mozi.backend.global.health;

import com.mozi.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 서버 동작 여부를 확인하는 헬스체크 엔드포인트.
 *
 * 인프라(로드밸런서, AWS Health Check) 또는 개발 중 서버가 떠 있는지
 * 빠르게 검증할 때 사용한다. 비인증 엔드포인트라 SecurityConfig 작성 후에도
 * permitAll로 열어둔다.
 */
@Tag(name = "Health", description = "서버 헬스체크 — 비인증")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * GET /api/health — 서버가 응답 가능한 상태인지 확인.
     *
     * 현재는 Spring Boot가 떠 있다는 사실만 검증.
     * 추후 DB 커넥션 / 챗봇 서버 연결 상태 등을 체크 항목으로 추가할 수 있다.
     *
     * @return 항상 { "status": "UP" } 페이로드를 담은 성공 응답
     */
    @Operation(summary = "헬스체크", description = "서버가 응답 가능한 상태인지 확인. { status: UP } 반환.")
    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
// 이 클래스의 역할: "서버 살아있어?"의 단순 응답.
// API_SPEC #1 기준 비인증 엔드포인트이며, 통일 응답 포맷이 적용됐는지 확인하는 첫 검증 지점이기도 함.
