package com.mozi.backend.domain.region.controller;

import com.mozi.backend.domain.region.service.RegionService;
import com.mozi.backend.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 행정구역 마스터 조회 라우트(`/api/regions`)의 단일 진입점.
 *
 * 비로그인도 접근 가능한 공개 라우트(SecurityConfig permitAll). 응답 형태가
 * sido 파라미터 유무로 분기되는 변형 엔드포인트 1종:
 *  - GET /api/regions          → 전체를 시도별로 묶어 반환 (List&lt;SidoGroupDto&gt;)
 *  - GET /api/regions?sido=11  → 해당 시도의 시군구 평면 목록 (List&lt;RegionDto&gt;)
 *
 * 응답 두 형태가 다르므로 반환 타입은 와일드카드. OpenAPI 스키마 표현은
 * Step 10 문서화 단계에서 명시한다.
 */
@Tag(name = "Region", description = "행정구역 마스터 조회 — 비로그인 접근 가능")
@RestController
@RequestMapping("/api/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    /**
     * 행정구역 목록 조회.
     *
     * sido 미지정 시 시도별 그루핑 응답, 지정 시 해당 시도의 시군구 평면 응답.
     * 시드 적재(Step 8) 전이면 양쪽 모두 빈 배열을 반환한다.
     *
     * @param sido 시도 코드 (예: "11" = 서울특별시). 미지정이면 null
     * @return 시도 미지정이면 SidoGroupDto[] 형태, 지정이면 RegionDto[] 형태
     */
    @Operation(
            summary = "행정구역 목록 조회",
            description = "sido 파라미터 미지정 시 시도별 그루핑 응답, 지정 시 해당 시도의 시군구 평면 목록 응답."
    )
    @GetMapping
    public ApiResponse<?> getRegions(@RequestParam(required = false) String sido) {
        if (sido == null || sido.isBlank()) {
            return ApiResponse.success(regionService.getAllGroupedBySido());
        }
        return ApiResponse.success(regionService.getSigungusBySido(sido));
    }
}
// 이 클래스의 역할: 행정구역 마스터 조회 단일 엔드포인트.
// sido 파라미터 유무로 응답 형태가 갈라지는 점이 Category와의 차이.
// 시드 적재 전이라 본 Step에선 빈 응답이 정상 — Step 8 시드 적재 후 실데이터가 채워진다.
