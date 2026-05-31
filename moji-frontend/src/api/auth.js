// api/auth.js
//
// 인증 도메인 API 함수. baseFetch 위에 얹은 얇은 래퍼.
// signup/login은 토큰이 아직 없으므로 skipAuth=true 로 호출.
// refresh는 client.js 내부에서만 호출하므로 별도 export하지 않는다.

import { baseFetch } from "./client";

/**
 * 회원가입
 *
 * @param {Object} args
 * @param {string} args.email
 * @param {string} args.password - 8~72자
 * @returns {Promise<{userId:number, accessToken:string, refreshToken:string, tokenType:"Bearer", expiresIn:number}>}
 * @throws {ApiError} VALIDATION_FAILED | EMAIL_ALREADY_EXISTS
 */
export function signup({ email, password }) {
  return baseFetch("/api/auth/signup", {
    method: "POST",
    body: { email, password },
    skipAuth: true,
  });
}

/**
 * 로그인
 *
 * @param {Object} args
 * @param {string} args.email
 * @param {string} args.password
 * @returns {Promise<{accessToken:string, refreshToken:string, tokenType:"Bearer", expiresIn:number}>}
 * @throws {ApiError} VALIDATION_FAILED | INVALID_CREDENTIALS
 */
export function login({ email, password }) {
  return baseFetch("/api/auth/login", {
    method: "POST",
    body: { email, password },
    skipAuth: true,
  });
}

/**
 * 로그아웃 (백엔드 측 refreshToken 무효화)
 *
 * @returns {Promise<{loggedOut:boolean}>}
 */
export function logout() {
  return baseFetch("/api/auth/logout", { method: "POST" });
}
