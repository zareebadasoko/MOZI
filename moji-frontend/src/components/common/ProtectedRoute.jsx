// components/common/ProtectedRoute.jsx
//
// 보호 라우트 래퍼. 비로그인 사용자가 보호 경로에 진입하면 /login으로 자동 이동.
//
// 두 가지 흐름 구분 (AuthContext.intentionalLogout 으로 판단):
//  1) 의도적 로그아웃 (헤더/마이페이지 로그아웃, 회원 탈퇴, 비밀번호 변경 성공)
//     → redirect 쿼리 미부착. LoginPage 가 기본 "/" 로 보냄 (재로그인 후 홈).
//  2) 무자격 접근 (공유 링크, 세션 만료로 인한 자동 로그아웃, 직접 URL 진입)
//     → 원래 가려던 경로를 redirect 쿼리로 보존. LoginPage 가 로그인 후 그쪽으로 복귀.
//
// 부트스트랩 중(새로고침 직후 토큰 복구 시도) Spinner 표시.

import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { getRefreshToken } from "../../utils/tokenStore";
import Spinner from "./Spinner";

/**
 * ProtectedRoute
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children - 보호할 페이지 컴포넌트
 * @returns {JSX.Element}
 */
export function ProtectedRoute({ children }) {
  const { isAuthenticated, bootstrapping, intentionalLogout } = useAuth();
  const location = useLocation();

  if (bootstrapping) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner label="잠시만요…" />
      </div>
    );
  }

  // login() 직후 보호 라우트로 즉시 navigate 하는 흐름(회원가입 모달 "지금 입력하기" 등)에서
  // setAuthenticated(true) 가 commit 되기 전에 ProtectedRoute 가 재평가돼 /login 으로 튕기는 race 방어.
  // tokenStore 는 모듈 변수라 setRefreshInStore 직후 동기적으로 truthy → React commit 과 무관하게 관찰 가능.
  // 부트스트랩 실패·로그아웃·토큰 만료 흐름 모두 clearTokens() 를 호출하므로 false negative 위험 없음.
  if (!isAuthenticated && !getRefreshToken()) {
    // 의도적 로그아웃 흐름: 사용자가 일부러 떠난 자리에 redirect 메모를 남겨두면
    // 다음 로그인 때 그 자리로 다시 돌려보내져 어색하다. 쿼리 없이 /login 으로만.
    if (intentionalLogout) {
      return <Navigate to="/login" replace />;
    }
    // 무자격 접근(공유 링크/세션 만료 등): 원래 가려던 경로를 redirect 쿼리로 보존.
    // 로그인 성공 후 LoginPage 가 이 쿼리를 읽어 navigate.
    const redirect = encodeURIComponent(location.pathname + location.search);
    return <Navigate to={`/login?redirect=${redirect}`} replace />;
  }

  return children;
}
