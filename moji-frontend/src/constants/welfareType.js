// constants/welfareType.js
//
// 백엔드 welfareType enum(CENTRAL/LOCAL/PRIVATE/SEOUL) 4종의 한글 라벨 매핑.
// 백엔드는 enum 상수명만 반환하므로 화면 표시용 라벨은 프론트가 관리한다.
// 출처 뱃지(WelfareCard), 검색 필터의 ChipGroup, 상세 페이지 등에서 재사용.

/**
 * welfareType enum → 한글 라벨
 * CATEGORY_REFERENCE.md / API_SPEC.md 의 welfareType 열거값과 정확히 일치해야 한다.
 */
export const WELFARE_TYPE_LABEL = {
  CENTRAL: "중앙부처",
  LOCAL: "지자체",
  PRIVATE: "민간",
  SEOUL: "서울시",
};

/**
 * UI 정렬 순서. ChipGroup·셀렉트 옵션을 만들 때 사용.
 * 중앙 → 지자체 → 민간 → 서울 순서가 사용자에게 가장 익숙하다는 가정.
 */
export const WELFARE_TYPE_ORDER = ["CENTRAL", "LOCAL", "PRIVATE", "SEOUL"];
