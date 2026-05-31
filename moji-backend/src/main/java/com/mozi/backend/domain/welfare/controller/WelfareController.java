package com.mozi.backend.domain.welfare.controller;

import com.mozi.backend.domain.welfare.dto.WelfareDetailDto;
import com.mozi.backend.domain.welfare.dto.WelfareSearchCondition;
import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;
import com.mozi.backend.domain.welfare.entity.WelfareType;
import com.mozi.backend.domain.welfare.service.WelfareService;
import com.mozi.backend.global.common.ApiResponse;
import com.mozi.backend.global.common.PageResponse;
import com.mozi.backend.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 복지 검색·상세 라우트(`/api/welfares`)의 단일 진입점.
 *
 * 두 엔드포인트 모두 SecurityConfig에서 permitAll로 열려 있고 Optional auth 처리:
 *  - 로그인 시 SecurityContext의 AuthenticatedUser가 채워짐 → 본인 북마크(isBookmarked) 반영
 *  - 비로그인 시 principal=null → isBookmarked 모두 false
 *
 * Step 6(USER_PROFILE_REDESIGN §5 Step 6): 옵션 C 채택으로 본인 프로필 자동 반영(applyMyProfile)
 * 정책은 제거되었다. 검색은 사용자가 보낸 query param(keyword/category/region/welfareType)만
 * 사용하며, 본인 프로필 기반 추천은 챗봇 흐름이 전담.
 *
 * 검색 파라미터는 모두 선택사항이며 기본값은 컨트롤러 단계에서 적용.
 */
@Tag(name = "Welfare", description = "복지 검색·상세 — 비로그인 접근 가능 (로그인 시 본인 북마크 반영)")
@RestController
@RequestMapping("/api/welfares")
@RequiredArgsConstructor
public class WelfareController {

    private final WelfareService welfareService;

    /**
     * 동적 검색 + 페이지네이션.
     *
     * 모든 필터는 사용자가 명시한 query param만 사용한다.
     *
     * @param keyword 키워드 (title/summary LIKE) — 선택
     * @param category 단일 카테고리 코드 — 선택
     * @param region 지역명 (LOCAL 자식에만 적용) — 선택
     * @param welfareType 출처 필터 — 선택
     * @param page 0-based 페이지 (기본 0)
     * @param size 페이지 크기 (기본 10, 서비스에서 1~50으로 클램프)
     * @param principal 인증 사용자 (비로그인 null — isBookmarked는 모두 false)
     * @return 페이지 응답
     */
    @Operation(summary = "복지 검색 (동적 필터링)", description = "키워드(title/summary LIKE), 카테고리, 지역(LOCAL만), 출처별 필터 + 페이지네이션. 로그인 시 isBookmarked만 자동 반영.")
    @GetMapping
    public ApiResponse<PageResponse<WelfareSummaryDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) WelfareType welfareType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        WelfareSearchCondition condition = new WelfareSearchCondition(
                keyword, category, region, welfareType, page, size);
        return ApiResponse.success(welfareService.search(condition, principal));
    }

    /**
     * 복지 상세 조회.
     *
     * @param id 자연키 (WLF/BOK/SEL prefix)
     * @param principal 인증 사용자 (비로그인 null — isBookmarked는 false)
     * @return 부모 + 자식 detail 1개 + categories + isBookmarked
     */
    @Operation(summary = "복지 상세 조회", description = "부모(WelfareCommon) + 자식 detail 1개 + 카테고리 목록 + (로그인 시) 본인 북마크 여부.")
    @GetMapping("/{id}")
    public ApiResponse<WelfareDetailDto> getDetail(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(welfareService.getDetail(id, principal));
    }
}
// 이 클래스의 역할: HTTP ↔ WelfareService 어댑터.
// 비즈니스 로직은 모두 서비스 레이어에 있고 컨트롤러는 라우팅·기본값·응답 래핑만 담당한다.
// Step 6에서 applyMyProfile 제거 — 본인 프로필 자동 반영 정책은 사라지고 검색은 명시 query param만 사용.
