package com.mozi.backend.domain.category.entity;

/**
 * 카테고리 분류 enum.
 *
 * THEME(관심 주제)와 STATUS(가구 상황) 두 종류로 카테고리를 구분한다.
 * 크롤링 데이터의 두 개 콤마 분리 컬럼(`interest_theme_code`,
 * `household_status_code`)이 각각 THEME / STATUS로 매핑되며, 두 부류는
 * UI 단계에서도 분리 표시된다 (예: 관심 주제 필터 vs 가구 상황 필터).
 */
public enum CategoryType {
    THEME,
    STATUS
}
// 이 enum의 역할: Category의 두 부류 분리 표시.
// `@Enumerated(EnumType.STRING)`으로 매핑되어 DB에 "THEME" / "STATUS" 문자열 저장.
