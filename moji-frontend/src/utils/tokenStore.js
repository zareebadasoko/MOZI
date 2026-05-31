// utils/tokenStore.js
//
// 토큰 저장소 — 단일 출처(single source of truth).
// CLAUDE.md §4-5 토큰 정책의 구체 구현:
//  - accessToken : 메모리(모듈 변수)        새로고침 시 사라지는 게 의도. refresh로 즉시 복구.
//  - refreshToken: sessionStorage           탭 닫으면 자동 삭제, 새로고침은 유지.
//
// 절대 localStorage에 토큰을 저장하지 않는다 (XSS 영구 노출 위험).

let accessToken = null;

const REFRESH_KEY = "mozi_refresh_token";

/**
 * accessToken 가져오기 (메모리)
 *
 * @returns {string|null}
 */
export function getAccessToken() {
  return accessToken;
}

/**
 * accessToken 메모리에 저장 (또는 null 시 비우기)
 *
 * @param {string|null} token
 */
export function setAccessToken(token) {
  accessToken = token;
}

/**
 * refreshToken 가져오기 (sessionStorage)
 *
 * @returns {string|null}
 */
export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_KEY);
}

/**
 * refreshToken sessionStorage에 저장 (또는 null 시 삭제)
 *
 * @param {string|null} token
 */
export function setRefreshToken(token) {
  if (token) sessionStorage.setItem(REFRESH_KEY, token);
  else sessionStorage.removeItem(REFRESH_KEY);
}

/**
 * 두 토큰 모두 클리어 (로그아웃 / 회원 탈퇴 / refresh 실패 시)
 */
export function clearTokens() {
  setAccessToken(null);
  setRefreshToken(null);
}

// 메모리 변수를 직접 export하지 않고 getter/setter를 노출하는 이유:
// React 외 코드(api/client.js)에서도 동일하게 접근하기 위함이며,
// 추후 zustand로 이주해도 이 인터페이스만 유지하면 호출부 영향 없음.
