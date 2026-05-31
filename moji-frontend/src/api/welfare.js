// api/welfare.js
//
// 복지 도메인 API 함수. baseFetch만 거치며 fetch는 직접 사용하지 않는다.
// 백엔드 API_SPEC.md §4(welfares) 참고.

import { baseFetch } from "./client";

/**
 * 복지 목록 검색
 *
 * 백엔드는 keyword/category/region/welfareType + page/size 쿼리만 받는다.
 * 2026-05-17 USER_PROFILE_REDESIGN 으로 applyMyProfile 파라미터는 제거되었음(FRONTEND_MIGRATION_NOTES.md §2-4).
 * 본인 맞춤 추천이 필요하면 챗봇(POST /api/chat)을 사용한다.
 *
 * 비로그인도 호출 가능 — 응답의 isBookmarked 는 항상 false 로 옴.
 *
 * @param {Object} [params]
 * @param {string} [params.keyword] - title/summary LIKE 검색
 * @param {string} [params.category] - 단일 카테고리 코드 (예: "THM005")
 * @param {string} [params.region] - 지역 자유 텍스트 (예: "강남구", "경기도")
 * @param {"CENTRAL"|"LOCAL"|"PRIVATE"|"SEOUL"} [params.welfareType]
 * @param {number} [params.page=0] - 0-based, 서버 0~50 클램프
 * @param {number} [params.size=10] - 1~50 클램프
 * @returns {Promise<{ items: Array, page: number, size: number, totalCount: number, hasNext: boolean }>}
 * @throws {ApiError} VALIDATION_FAILED | NETWORK_ERROR | INTERNAL_ERROR
 */
export function fetchWelfares(params = {}) {
  return baseFetch("/api/welfares", { query: params });
}

/**
 * 복지 상세 조회
 *
 * Phase 4-2 에서 본격적으로 사용한다. 4-1 에서는 카드 클릭 라우팅 동작만 확인.
 * id 는 영숫자 자연키(예: WLF00001234) 라 보통은 인코딩이 필요 없지만,
 * 외부 시스템에서 들어오는 ID 표류에 대비해 encodeURIComponent 처리.
 *
 * @param {string} id - 복지 ID (예: "WLF00001234")
 * @returns {Promise<Object>} WelfareDetailDto (출처별 자식 detail 포함)
 * @throws {ApiError} WELFARE_NOT_FOUND | NETWORK_ERROR
 */
export function fetchWelfareDetail(id) {
  return baseFetch(`/api/welfares/${encodeURIComponent(id)}`);
}
