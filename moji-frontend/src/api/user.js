// api/user.js
//
// 사용자 도메인 API 함수.
// Phase 2에선 getMyProfile만 실제로 호출된다 (AuthProvider 부트스트랩의 가벼운 호출용).
// 나머지(updateMyProfile, changePassword, withdraw)는 Phase 4에서 그대로 사용.

import { baseFetch } from "./client";

/**
 * 내 프로필 조회
 *
 * 응답 스키마는 2026-05-17 USER_PROFILE_REDESIGN 이후 다음 형태로 통일됨
 * (자세한 내용은 ../backend_project/docs/FRONTEND_MIGRATION_NOTES.md §2-1 참조):
 *
 *  - 프로필 미입력 상태:
 *      { isCompleted: false }
 *
 *  - 프로필 입력 완료:
 *      {
 *        isCompleted: true,
 *        sidoCode,        // VARCHAR(2) — 행정구역 시도 코드. 라벨은 GET /api/regions 응답으로 매핑
 *        sigunguCode,     // VARCHAR(5) | null — 세종특별자치시는 null
 *        incomeType,      // enum value: NATIONAL_BASIC_LIVING | NEAR_POVERTY | BASIC_PENSION | GENERAL | UNKNOWN
 *        householdType,   // enum value: LIVING_ALONE | COUPLE | WITH_CHILDREN | GRANDPARENT_GRANDCHILD | OTHER
 *        isDisabled, isMultiChild, isMulticulturalNorthDefector, isVeteran  // boolean 4종
 *      }
 *
 * 한글 라벨 매핑(INCOME_TYPE_LABEL / HOUSEHOLD_TYPE_LABEL)은 Phase 4에서 src/constants/profileEnums.js 로 분리 예정.
 *
 * @returns {Promise<Object>}
 */
export function getMyProfile() {
  return baseFetch("/api/users/me/profile");
}

/**
 * 내 프로필 부분 수정
 *
 * 부분 갱신 정책(옵션 C): 요청에 없는 필드는 변경하지 않음.
 * 명시적 null 도 동일하게 "변경 안 함"으로 취급된다(absent와 구분 불가) → 호출자가 미리 정리해 보낸다.
 *
 * 보낼 수 있는 필드 (FRONTEND_MIGRATION_NOTES.md §2-2):
 *   sidoCode, sigunguCode, incomeType, householdType,
 *   isDisabled, isMultiChild, isMulticulturalNorthDefector, isVeteran
 *
 * 지역 코드가 유효하지 않으면 백엔드가 INVALID_REGION_CODE(400)을 던진다 → 호출자가 토스트 + 시군구 select 초기화.
 *
 * @param {Object} partial - 변경할 필드만 담은 객체
 * @returns {Promise<Object>} 갱신된 프로필
 * @throws {ApiError} INVALID_REGION_CODE | VALIDATION_FAILED
 */
export function updateMyProfile(partial) {
  return baseFetch("/api/users/me/profile", {
    method: "PUT",
    body: partial,
  });
}

/**
 * 비밀번호 변경
 *
 * 성공 시 백엔드가 모든 refreshToken을 무효화한다 → 본 기기 포함 모든 기기에서 재로그인 필요.
 * 호출 후 프론트는 즉시 clearTokens() + /login 이동을 권장 (CLAUDE.md §4-5).
 *
 * @param {Object} args
 * @param {string} args.currentPassword
 * @param {string} args.newPassword
 * @returns {Promise<{changed:boolean}>}
 * @throws {ApiError} PASSWORD_MISMATCH | SAME_PASSWORD | VALIDATION_FAILED
 */
export function changePassword({ currentPassword, newPassword }) {
  return baseFetch("/api/users/me/password", {
    method: "PUT",
    body: { currentPassword, newPassword },
  });
}

/**
 * 회원 탈퇴
 *
 * 성공 시 모든 토큰이 백엔드에서 무효화된다.
 * 호출 후 프론트는 clearTokens() + /login 이동.
 *
 * @returns {Promise<Object>}
 */
export function withdraw() {
  return baseFetch("/api/users/me", { method: "DELETE" });
}
