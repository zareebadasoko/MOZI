// contexts/AuthContext.jsx
//
// 로그인 상태(isAuthenticated)를 React 트리 전체에 흘려주는 Context.
// tokenStore.js는 모듈 변수라 어디서든 접근 가능하지만, "토큰 변경 시 화면 리렌더"가 필요한
// 부분(헤더 메뉴, ProtectedRoute 등)은 React state여야 한다 → 그 통로가 이 Context.
//
// 새로고침 시 토큰 복구 흐름:
//  1) AuthProvider mount
//  2) sessionStorage에 refreshToken이 있으면 baseFetch("/api/users/me/profile") 호출
//  3) 401 응답이 TOKEN_EXPIRED면 client.js가 자동 refresh + 재시도 → 성공
//  4) 응답 성공 시 setAuthenticated(true)
//  5) 실패 시 clearTokens() (조용히 로그아웃 상태로 남음)

import { createContext, useContext, useEffect, useRef, useState } from "react";
import {
  setAccessToken as setTokenInStore,
  setRefreshToken as setRefreshInStore,
  getRefreshToken,
  clearTokens,
} from "../utils/tokenStore";
import { clearConversationId } from "../utils/conversationStore";
import { clearChatMessages } from "../utils/chatHistoryStore";
import { baseFetch } from "../api/client";

const AuthContext = createContext(null);

/**
 * AuthProvider
 *
 * 앱 최상단에서 한 번만 감싼다. 새로고침 직후 토큰 복구를 1회 시도한다.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children
 * @returns {JSX.Element}
 */
export function AuthProvider({ children }) {
  const [isAuthenticated, setAuthenticated] = useState(false);
  const [bootstrapping, setBootstrapping] = useState(true);

  // 의도적 로그아웃 플래그.
  //  · true 가 되면 ProtectedRoute 가 redirect 쿼리를 부착하지 않고 /login 으로 보낸다.
  //  · 의도적 로그아웃(헤더/마이페이지 로그아웃, 회원 탈퇴, 비밀번호 변경)에서 true.
  //  · 무자격 접근(공유 링크·세션 만료) 에서는 false 그대로라 기존처럼 redirect 부착.
  //  · 다음 로그인 시 login() 이 false 로 리셋.
  // 본 플래그가 필요한 이유: react-router 7 + react 19 의 자동 배치만으로는
  //   "auth=false 가 적용된 직후 ProtectedRoute 가 현재 경로를 redirect 쿼리에 부착해
  //    /login?redirect=... 로 보내버리는" 경합이 안 막힘. setAuthenticated 와 같은 commit
  //   에서 함께 반영되는 state 로 의도를 명시해 ProtectedRoute 가 분기하게 한다.
  const [intentionalLogout, setIntentionalLogout] = useState(false);

  // React 18 StrictMode는 개발 모드에서 useEffect를 2회 실행한다.
  // 두 번째 실행이 첫 번째의 refresh로 회전된 토큰을 다시 refresh 시도 → INVALID_REFRESH_TOKEN
  // 위험이 있으므로 ref 가드로 1회만 동작하도록 막는다 (API_CLIENT_GUIDE §10-5 참고).
  const bootstrapStartedRef = useRef(false);

  // 새로고침 시 토큰 복구 시도 (마운트 시 1회).
  // baseFetch 내부의 tryRefresh를 직접 노출하지 않으므로, 가벼운 호출 한 번으로 자동 갱신을 트리거한다.
  useEffect(() => {
    if (bootstrapStartedRef.current) return;
    bootstrapStartedRef.current = true;

    const refresh = getRefreshToken();
    if (!refresh) {
      setBootstrapping(false);
      return;
    }
    baseFetch("/api/users/me/profile")
      .then(() => setAuthenticated(true))
      .catch(() => clearTokens())
      .finally(() => setBootstrapping(false));
  }, []);

  /**
   * 로그인 / 가입 성공 시 호출. 토큰 저장 + 인증 상태 true.
   * 이전에 의도적 로그아웃 플래그가 켜져 있었다면 함께 리셋한다.
   *
   * @param {string} accessToken
   * @param {string} refreshToken
   */
  const login = (accessToken, refreshToken) => {
    setTokenInStore(accessToken);
    setRefreshInStore(refreshToken);
    // 새 로그인은 무조건 깨끗한 시작. 이전 세션의 챗봇 메시지가 남아있을 가능성을 차단.
    clearChatMessages();
    setIntentionalLogout(false);
    setAuthenticated(true);
  };

  /**
   * 로그아웃. 두 토큰 + conversationId + 챗봇 메시지 히스토리까지 모두 클리어 (CLAUDE.md §4-5).
   * intentionalLogout=true 로 ProtectedRoute 가 redirect 쿼리를 부착하지 않도록 신호.
   */
  const logout = () => {
    clearTokens();
    clearConversationId();
    clearChatMessages();
    setIntentionalLogout(true);
    setAuthenticated(false);
  };

  return (
    <AuthContext.Provider
      value={{ isAuthenticated, bootstrapping, intentionalLogout, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/**
 * useAuth
 *
 * 컴포넌트는 AuthContext를 직접 import하지 않고 이 훅을 사용한다.
 * <AuthProvider> 밖에서 호출하면 명시적으로 throw — 디버깅 친화.
 *
 * @returns {{ isAuthenticated: boolean, bootstrapping: boolean, intentionalLogout: boolean, login: Function, logout: Function }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within <AuthProvider>");
  return ctx;
}
