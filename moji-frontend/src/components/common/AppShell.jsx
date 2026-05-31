// components/common/AppShell.jsx
//
// 모든 페이지가 공유하는 골격: 헤더 + 본문 + 푸터 + 토스트 영역.
// 본문 폭은 페이지마다 다를 수 있으므로 여기서는 강제하지 않고 페이지가 직접 max-w 지정.

import AppHeader from "./AppHeader";
import AppFooter from "./AppFooter";
import ToastContainer from "./ToastContainer";

/**
 * AppShell
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children
 * @returns {JSX.Element}
 */
export default function AppShell({ children }) {
  return (
    <div className="min-h-screen flex flex-col bg-surface-muted">
      <AppHeader />

      <div className="flex-1 w-full">
        {children}
      </div>

      <AppFooter />
      <ToastContainer />
    </div>
  );
}
