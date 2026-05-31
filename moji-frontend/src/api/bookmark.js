// api/bookmark.js
//
// 북마크 도메인 API. baseFetch만 거치며 fetch 직접 사용 X.
// 백엔드 API_SPEC.md §6 참고.

import { baseFetch } from "./client";

/**
 * 북마크 추가 (멱등)
 *
 * 이미 저장되어 있어도 같은 bookmarkId를 200으로 돌려준다 — 재시도 안전.
 *
 * @param {string} welfareId
 * @returns {Promise<{ bookmarkId: number }>}
 * @throws {ApiError} UNAUTHORIZED | WELFARE_NOT_FOUND | NETWORK_ERROR
 */
export function createBookmark(welfareId) {
  return baseFetch(`/api/bookmarks/${encodeURIComponent(welfareId)}`, {
    method: "POST",
  });
}

/**
 * 북마크 삭제
 *
 * 이미 없는 북마크 삭제 시도는 BOOKMARK_NOT_FOUND(404) — 호출자가 UI 상태만 동기화.
 *
 * @param {string} welfareId
 * @returns {Promise<{ deleted: true }>}
 * @throws {ApiError} UNAUTHORIZED | WELFARE_NOT_FOUND | BOOKMARK_NOT_FOUND | NETWORK_ERROR
 */
export function deleteBookmark(welfareId) {
  return baseFetch(`/api/bookmarks/${encodeURIComponent(welfareId)}`, {
    method: "DELETE",
  });
}

/**
 * 내 북마크 목록 (페이지)
 *
 * 응답은 검색과 동일한 PageResponse<WelfareSummaryDto>.
 * 본 함수는 Phase 4-3 BookmarksPage 에서 본격 사용. 4-2 에서는 export 만.
 *
 * @param {Object} [params]
 * @param {number} [params.page=0]
 * @param {number} [params.size=10]
 * @returns {Promise<{ items: Array, page: number, size: number, totalCount: number, hasNext: boolean }>}
 * @throws {ApiError} UNAUTHORIZED | NETWORK_ERROR
 */
export function fetchBookmarks({ page = 0, size = 10 } = {}) {
  return baseFetch("/api/bookmarks", { query: { page, size } });
}
