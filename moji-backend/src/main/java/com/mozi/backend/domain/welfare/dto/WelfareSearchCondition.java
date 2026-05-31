package com.mozi.backend.domain.welfare.dto;

import com.mozi.backend.domain.welfare.entity.WelfareType;

/**
 * GET /api/welfares 검색 조건 묶음 record.
 *
 * 컨트롤러가 @RequestParam들을 받아 본 record로 한 번에 묶은 뒤 서비스에 전달한다.
 * 메서드 시그니처가 6개 파라미터로 폭발하지 않게 하기 위함이며, 모든 파라미터는 선택사항이다.
 *
 * Step 6 재설계 (USER_PROFILE_REDESIGN §5 Step 6): 옵션 C 채택으로
 * `applyMyProfile` 필드 제거 — 검색은 사용자가 명시한 query param만 사용한다.
 * 본인 프로필 기반 자동 필터링은 챗봇 컨텍스트 흐름이 전담.
 *
 * - keyword/category/region: null 또는 blank 허용. Specification 단계에서 무시 처리.
 * - welfareType: null이면 전체 출처 검색
 * - page/size: 컨트롤러 기본값 (0, 10) 적용 후 서비스에서 size 클램프(1~50)
 *
 * @param keyword 키워드 (title + summary LIKE)
 * @param category 단일 카테고리 코드
 * @param region 지역명 (LOCAL 자식에만 적용)
 * @param welfareType 출처 필터
 * @param page 0-based 페이지 번호
 * @param size 페이지 크기 (서비스에서 1~50 범위로 클램프)
 */
public record WelfareSearchCondition(
        String keyword,
        String category,
        String region,
        WelfareType welfareType,
        int page,
        int size
) {
}
// 이 record의 역할: 검색 파라미터를 단일 객체로 묶어 컨트롤러 ↔ 서비스 시그니처를 단순화.
// Step 6에서 applyMyProfile 제거 — 검색에서 본인 프로필 자동 반영은 더 이상 수행하지 않는다.
