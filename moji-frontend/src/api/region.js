// api/region.js
//
// 행정구역 마스터 조회. 백엔드 시드(2026-05-17 USER_PROFILE_REDESIGN) 적재 후
// 17 시도 + 약 229 시군구가 한 번에 응답되므로 모듈 변수에 Promise 자체를 캐싱한다.
// fetchCategories(src/api/category.js) 와 동일한 패턴.
//
// Phase 4-1: 검색 페이지의 시도/시군구 chip 데이터로 사용 (이름 기반 region 쿼리에 매핑).
// Phase 4-4: ProfilePage 의 cascading select 가 같은 데이터를 sidoCode/sigunguCode 로 소비할 예정.

import { baseFetch } from "./client";

/**
 * 캐시된 트리 응답 Promise. 인자 없는 호출용.
 * @type {Promise<Array> | null}
 */
let cachedTreePromise = null;

/**
 * 시도/시군구 트리 조회 (메모이제이션)
 *
 * 응답 형태 (../backend_project/docs/API_SPEC.md §5-2 + FRONTEND_MIGRATION_NOTES.md §2-5):
 * [
 *   { sidoCode: "11", sidoName: "서울특별시",
 *     sigungus: [ { code: "11110", name: "종로구" }, ... ] },
 *   { sidoCode: "36", sidoName: "세종특별자치시",
 *     sigungus: [ { code: null, name: null } ] },   // 세종은 시군구 없음 표시
 *   ...
 * ]
 *
 * @returns {Promise<Array<{ sidoCode: string, sidoName: string, sigungus: Array<{ code: string|null, name: string|null }> }>>}
 * @throws {ApiError} NETWORK_ERROR 등
 */
export function getRegions() {
  if (cachedTreePromise) return cachedTreePromise;

  cachedTreePromise = baseFetch("/api/regions").catch((err) => {
    // 실패 Promise 를 캐시에 남기면 영원히 재시도 불가 → 제거
    cachedTreePromise = null;
    throw err;
  });
  return cachedTreePromise;
}

/**
 * 테스트/세션 정리용 캐시 비우기. MVP 단계에선 호출처 없음.
 */
export function clearRegionCache() {
  cachedTreePromise = null;
}
