// api/client.js
//
// 모든 백엔드 호출이 거쳐가는 단일 진입점.
//  - Authorization 헤더 자동 부착 (skipAuth=true면 생략)
//  - JSON 직렬화/역직렬화 자동
//  - ApiResponse<T> 래퍼 해석: status === "SUCCESS" → data 반환, "ERROR" → ApiError throw
//  - errorCode === "TOKEN_EXPIRED" 시 1회만 자동 refresh 후 원 요청 재시도
//
// 컴포넌트는 이 모듈을 직접 import하지 않는다. 항상 api/auth.js, api/user.js 등을 거친다.

import {
  getAccessToken,
  setAccessToken,
  getRefreshToken,
  setRefreshToken,
  clearTokens,
} from "../utils/tokenStore";

const API_BASE = import.meta.env.VITE_API_BASE_URL;

/**
 * ApiError
 *
 * 백엔드 에러 응답을 자바스크립트 Error로 변환한 클래스.
 * 컴포넌트는 errorCode로 분기한다 (../backend_project/docs/ERROR_CODES.md 참조).
 */
export class ApiError extends Error {
  constructor({ errorCode, message, fields, httpStatus }) {
    super(message || errorCode);
    this.errorCode = errorCode;
    this.fields = fields || null; // VALIDATION_FAILED일 때만 채워짐
    this.httpStatus = httpStatus;
  }
}

/**
 * baseFetch
 *
 * 모든 API 호출이 거쳐가는 단일 진입점.
 *
 * @param {string} path - "/api/..." 로 시작하는 경로
 * @param {Object} [opts]
 * @param {string} [opts.method="GET"]
 * @param {Object} [opts.body] - JSON으로 직렬화될 객체 (없으면 body 미전송)
 * @param {Object} [opts.query] - URL query string으로 변환할 객체 (undefined/null/"" 자동 제거)
 * @param {boolean} [opts.skipAuth=false] - true면 Authorization 헤더 안 붙임 (signup/login/refresh)
 * @returns {Promise<any>} ApiResponse.data
 * @throws {ApiError}
 */
export async function baseFetch(path, opts = {}) {
  return doFetch(path, opts, /* retried */ false);
}

async function doFetch(path, opts, retried) {
  const { method = "GET", body, query, skipAuth = false } = opts;

  // 1) 쿼리스트링 구성 (undefined/null/"" 자동 제거, false는 보존)
  const url = buildUrl(path, query);

  // 2) 헤더 구성
  const headers = { "Content-Type": "application/json" };
  if (!skipAuth) {
    const token = getAccessToken();
    if (token) headers["Authorization"] = `Bearer ${token}`;
  }

  // 3) 호출
  let res;
  try {
    res = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    // 네트워크 끊김 등 — 원본 에러 객체를 사용하지 않고 우리 ApiError 로 정규화
    throw new ApiError({
      errorCode: "NETWORK_ERROR",
      message: "네트워크에 연결할 수 없어요. 잠시 후 다시 시도해주세요.",
      httpStatus: 0,
    });
  }

  // 4) 응답 파싱
  let json;
  try {
    json = await res.json();
  } catch {
    throw new ApiError({
      errorCode: "INVALID_RESPONSE",
      message: "서버 응답에 문제가 있어요. 잠시 후 다시 시도해주세요.",
      httpStatus: res.status,
    });
  }

  // 5) 성공
  if (json.status === "SUCCESS") return json.data;

  // 6) 에러 분기 — 401 계열 3종은 자동 refresh 후 1회 재시도
  //
  // 왜 3종 모두인가:
  //  - TOKEN_EXPIRED  : access 토큰이 만료된 정상 케이스
  //  - UNAUTHORIZED   : Authorization 헤더 자체가 없는 경우. 새로고침 직후 AuthProvider 부트스트랩에서
  //                     발생 (메모리의 accessToken이 사라진 상태) — refreshToken만 있으면 복구 가능해야 함
  //  - INVALID_TOKEN  : JWT 형식 오류/서명 불일치. 만료된 토큰이 부분 손상된 경우 → 보수적으로 refresh 시도
  //
  // refreshToken 자체가 없거나 만료라면 tryRefresh가 false 반환 → clearTokens.
  if (
    (json.errorCode === "TOKEN_EXPIRED" ||
      json.errorCode === "UNAUTHORIZED" ||
      json.errorCode === "INVALID_TOKEN") &&
    !retried &&
    !skipAuth
  ) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      return doFetch(path, opts, /* retried */ true);
    }
    // refresh 실패 시 토큰 모두 클리어 → 호출자가 라우팅 결정
    clearTokens();
  }

  throw new ApiError({
    errorCode: json.errorCode || "UNKNOWN",
    message: json.message || "알 수 없는 오류가 발생했어요.",
    fields: json.fields,
    httpStatus: res.status,
  });
}

/**
 * tryRefresh
 *
 * sessionStorage의 refreshToken으로 새 토큰 쌍 요청.
 *  - 성공: 새 accessToken/refreshToken 저장 후 true
 *  - 실패: false (refresh 토큰 없음/만료/위조)
 *
 * @returns {Promise<boolean>}
 */
async function tryRefresh() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const res = await fetch(`${API_BASE}/api/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    const json = await res.json();
    if (json.status !== "SUCCESS") return false;
    setAccessToken(json.data.accessToken);
    setRefreshToken(json.data.refreshToken);
    return true;
  } catch {
    return false;
  }
}

/** URL + query string 조립. undefined/null/"" 제거, boolean false는 보존(쿼리에서 "false" 문자열로 직렬화됨). */
function buildUrl(path, query) {
  const url = new URL(path, API_BASE);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== "") {
        url.searchParams.append(k, v);
      }
    });
  }
  return url.toString();
}
