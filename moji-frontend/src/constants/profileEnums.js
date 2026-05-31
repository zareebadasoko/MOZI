// constants/profileEnums.js
//
// 백엔드 user_profile 의 enum value 5종씩의 한글 라벨 매핑.
// 백엔드는 enum 상수명만 응답·수신하므로 화면 표시용 라벨은 프론트가 관리한다.
// 출처: backend_project/docs/FRONTEND_MIGRATION_NOTES.md §2-3 (2026-05-17 USER_PROFILE_REDESIGN).
//
// ProfilePage 의 Select 옵션, MyPage 의 표시 라벨 등에서 재사용.

/**
 * 소득 유형 enum → 한글 라벨
 */
export const INCOME_TYPE_LABEL = {
  NATIONAL_BASIC_LIVING: "기초생활수급자",
  NEAR_POVERTY: "차상위계층",
  BASIC_PENSION: "기초연금수급자",
  GENERAL: "해당 없음",
  UNKNOWN: "잘 모르겠어요",
};

/**
 * 가구 형태 enum → 한글 라벨
 *
 * 본 enum 이 옛 `isLivingAlone` (LIVING_ALONE) / `isSingleParentGrandparent`
 * (GRANDPARENT_GRANDCHILD) boolean 2개를 흡수했다. ProfilePage 의 토글에서는 제거.
 */
export const HOUSEHOLD_TYPE_LABEL = {
  LIVING_ALONE: "혼자 살아요 (독거)",
  COUPLE: "배우자와 둘이 살아요",
  WITH_CHILDREN: "자녀와 함께 살아요",
  GRANDPARENT_GRANDCHILD: "손주를 키우고 있어요 (조손)",
  OTHER: "그 외",
};

/**
 * UI 정렬 순서. select 옵션을 만들 때 사용.
 * 노년층에게 가장 익숙한/일반적인 항목을 위쪽으로.
 */
export const INCOME_TYPE_ORDER = [
  "NATIONAL_BASIC_LIVING",
  "NEAR_POVERTY",
  "BASIC_PENSION",
  "GENERAL",
  "UNKNOWN",
];

export const HOUSEHOLD_TYPE_ORDER = [
  "LIVING_ALONE",
  "COUPLE",
  "WITH_CHILDREN",
  "GRANDPARENT_GRANDCHILD",
  "OTHER",
];
