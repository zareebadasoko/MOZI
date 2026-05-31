// components/common/AppHeader.jsx
//
// 모든 페이지 상단의 공통 헤더. AppShell이 한 번만 렌더한다.
// 로고 영역(mozi-avatar.png + "MOZI" 워드마크)과 메인 네비, 로그인 상태별 메뉴.
//
// 로그아웃 흐름:
//  1) 백엔드 호출로 refreshToken 무효화 (실패해도 로컬 로그아웃은 진행)
//  2) AuthContext.logout() — 토큰 + conversationId 클리어
//  3) /로 navigate (replace로 뒤로가기 차단)

import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import { logout as apiLogout } from "../../api/auth";

const NAV_ITEMS = [
  { to: "/chat", label: "챗봇 상담" },
  { to: "/categories", label: "복지 분류" },
  { to: "/welfares", label: "복지 찾기" },
];

/**
 * AppHeader
 *
 * @returns {JSX.Element}
 */
export default function AppHeader() {
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    try {
      await apiLogout();
    } catch {
      // 서버 무효화 실패해도 로컬 세션은 끊음
    }
    navigate("/", { replace: true });
    logout();
  }

  return (
    <header className="bg-surface border-b border-surface-border sticky top-0 z-30 backdrop-blur-sm bg-surface/95">
      <div className="max-w-screen-xl mx-auto px-4 md:px-8 h-16 md:h-20 flex items-center justify-between gap-4">
        {/* 로고 — MOZI 캐릭터 이미지 + 워드마크 */}
        <Link
          to="/"
          aria-label="MOZI 홈으로"
          className="flex items-center gap-2 shrink-0"
        >
          <img
            src="/reference/mozi-avatar.png"
            alt=""
            className="h-10 w-10 md:h-12 md:w-12 object-contain"
          />
          <span className="text-senior-lg md:text-senior-xl font-extrabold text-brand-deep tracking-tight">
            MOZI
          </span>
        </Link>

        {/* 메인 네비게이션 — 데스크탑 노출 */}
        <nav aria-label="주요 메뉴" className="hidden md:flex items-center gap-1">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                "h-12 px-4 inline-flex items-center text-senior-base rounded-soft transition-colors " +
                (isActive
                  ? "text-brand-deep font-bold bg-brand-subtle"
                  : "text-ink-strong hover:bg-surface-muted")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* 인증 메뉴 */}
        <div className="flex items-center gap-1 sm:gap-2 shrink-0">
          {isAuthenticated ? (
            <>
              <Link
                to="/me"
                className="h-12 px-3 sm:px-4 inline-flex items-center text-senior-base text-ink-strong hover:bg-surface-muted rounded-soft"
              >
                마이페이지
              </Link>
              <button
                type="button"
                onClick={handleLogout}
                className="h-12 px-3 sm:px-4 text-senior-base text-ink-weak hover:bg-surface-muted rounded-soft"
              >
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="h-12 px-3 sm:px-4 inline-flex items-center text-senior-base text-ink-strong hover:bg-surface-muted rounded-soft"
              >
                로그인
              </Link>
              <Link
                to="/signup"
                className="h-12 px-4 inline-flex items-center text-senior-base font-bold text-ink-invert bg-brand hover:bg-brand-hover rounded-pill transition-colors"
              >
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>

      {/* 모바일 네비 (md 미만) */}
      <nav
        aria-label="주요 메뉴 (모바일)"
        className="md:hidden border-t border-surface-border bg-surface"
      >
        <div className="max-w-screen-xl mx-auto px-2 flex">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                "flex-1 h-12 inline-flex items-center justify-center text-senior-sm transition-colors " +
                (isActive
                  ? "text-brand-deep font-bold border-b-2 border-brand"
                  : "text-ink-weak")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </div>
      </nav>
    </header>
  );
}
