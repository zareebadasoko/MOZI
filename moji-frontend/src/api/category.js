// api/category.js
//
// THEME 15종 + STATUS 7종은 백엔드 마스터 데이터로 시드 적재 후 변하지 않는다.
// 같은 세션 내에서는 한 번만 받아두고 재사용하기 위해 모듈 변수에 캐싱한다.
// 동시에 여러 곳에서 호출돼도 fetch가 한 번만 일어나도록 Promise 자체를 보관한다.

import { baseFetch } from "./client";

/**
 * type ("THEME" | "STATUS") → Promise<Category[]>
 * Promise 자체를 캐싱하므로 동시 호출이 들어와도 백엔드에 1회만 다녀온다.
 * @type {Map<string, Promise<Array>>}
 */
const cache = new Map();

/**
 * 카테고리 목록 조회 (메모이제이션)
 *
 * 첫 호출: 백엔드에 요청 후 결과를 캐시에 보관.
 * 두 번째 호출부터: 캐시된 Promise 그대로 반환.
 * 실패 시 캐시 엔트리를 제거해 다음 호출에서 재시도 가능하게 한다.
 *
 * @param {"THEME"|"STATUS"} type
 * @returns {Promise<Array<{ code: string, name: string, type: "THEME"|"STATUS" }>>}
 * @throws {ApiError} VALIDATION_FAILED (잘못된 type) | NETWORK_ERROR
 */
export function fetchCategories(type) {
  if (cache.has(type)) return cache.get(type);

  const promise = baseFetch("/api/categories", { query: { type } }).catch(
    (err) => {
      // 실패한 Promise를 캐시에 남기면 영원히 재시도 불가 → 제거
      cache.delete(type);
      throw err;
    },
  );
  cache.set(type, promise);
  return promise;
}

/**
 * 테스트/로그아웃 등에서 캐시를 비울 필요가 있을 때 사용.
 * MVP 단계에선 호출처 없음.
 */
export function clearCategoryCache() {
  cache.clear();
}
