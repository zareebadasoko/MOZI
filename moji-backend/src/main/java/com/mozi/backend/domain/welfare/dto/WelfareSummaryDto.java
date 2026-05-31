package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.category.dto.CategoryDto;
import com.mozi.backend.domain.welfare.entity.WelfareCommon;
import com.mozi.backend.domain.welfare.entity.WelfareType;

import java.util.List;

/**
 * 검색·북마크 카드 목록의 단일 항목 DTO.
 *
 * GET /api/welfares 페이지 응답과 (Phase 4-3) GET /api/bookmarks 양쪽에서 재사용된다.
 * `categories`와 `isBookmarked`는 WelfareCommon 자체에 없는 파생 필드라
 * 호출자(WelfareService)가 N+1 회피 배치 lookup으로 채워서 of()에 주입한다.
 *
 * @param id 자연키 (예: "WLF00001234")
 * @param title 복지 제목
 * @param summary 한 줄 요약
 * @param welfareType 출처 (CENTRAL/LOCAL/PRIVATE/SEOUL)
 * @param organizationName 담당 기관명
 * @param categories 매핑된 카테고리 목록 (THEME + STATUS 혼합 가능)
 * @param isBookmarked 로그인 사용자의 북마크 여부 (비로그인 시 항상 false)
 */
public record WelfareSummaryDto(
        String id,
        String title,
        String summary,
        WelfareType welfareType,
        String organizationName,
        List<CategoryDto> categories,
        boolean isBookmarked
) {

    /**
     * WelfareCommon + 파생 필드(categories/isBookmarked)를 합쳐 응답 DTO 생성.
     *
     * @param welfare 영속화된 부모 엔티티 (자식 인스턴스여도 부모 필드만 사용)
     * @param categories 호출자가 배치 lookup으로 채운 카테고리 목록
     * @param isBookmarked 호출자가 배치 lookup으로 채운 북마크 여부
     * @return 응답 DTO
     */
    public static WelfareSummaryDto of(WelfareCommon welfare, List<CategoryDto> categories, boolean isBookmarked) {
        return new WelfareSummaryDto(
                welfare.getId(),
                welfare.getTitle(),
                welfare.getSummary(),
                welfare.getWelfareType(),
                welfare.getOrganizationName(),
                categories,
                isBookmarked
        );
    }
}
// 이 record의 역할: 검색·북마크 카드 응답의 단일 표현.
// of() 정적 팩토리는 WelfareCommon에 없는 파생 필드를 호출자가 주입하는 형태로 N+1 회피 패턴을 강제한다.
