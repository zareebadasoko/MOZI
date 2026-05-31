// App.jsx
//
// 라우터 매핑만 담당하는 최상위 컴포넌트.
// 모든 페이지가 AppShell 안에서 렌더되어 헤더·푸터·토스트 영역을 공유한다.

import { Routes, Route } from "react-router-dom";
import AppShell from "./components/common/AppShell";
import { ProtectedRoute } from "./components/common/ProtectedRoute";
import HomePage from "./pages/HomePage";
import ChatPage from "./pages/ChatPage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import WelfareCategoryPage from "./pages/WelfareCategoryPage";
import WelfareSearchPage from "./pages/WelfareSearchPage";
import WelfareDetailPage from "./pages/WelfareDetailPage";
import BookmarksPage from "./pages/BookmarksPage";
import MyPage from "./pages/MyPage";
import ProfilePage from "./pages/ProfilePage";
import PasswordPage from "./pages/PasswordPage";
import DevPage from "./pages/DevPage";
import NotFoundPage from "./pages/NotFoundPage";

/**
 * App
 *
 * Flowchart(MOZI_flowchart.png) 기반 라우트:
 *  - /                     공개         HomePage           (메인 화면, 챗봇/맞춤복지/뉴스 진입점)
 *  - /chat                 Protected   ChatPage           (챗봇 상담)
 *  - /login, /signup       공개         LoginPage / SignupPage
 *  - /categories           공개         WelfareCategoryPage (복지 분류 페이지)
 *  - /welfares             공개         WelfareSearchPage   (복지 목록/검색)
 *  - /welfares/:id         공개         WelfareDetailPage   (복지 상세)
 *  - /bookmarks            Protected   BookmarksPage      (저장한 복지)
 *  - /me                   Protected   MyPage             (마이페이지 허브)
 *  - /me/profile, /me/password   Protected   하위 페이지
 *  - *                     —           NotFoundPage
 *
 * @returns {JSX.Element}
 */
function App() {
  return (
    <AppShell>
      <Routes>
        <Route path="/" element={<HomePage />} />

        <Route
          path="/chat"
          element={
            <ProtectedRoute>
              <ChatPage />
            </ProtectedRoute>
          }
        />

        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />

        <Route path="/categories" element={<WelfareCategoryPage />} />
        <Route path="/welfares" element={<WelfareSearchPage />} />
        <Route path="/welfares/:id" element={<WelfareDetailPage />} />

        <Route
          path="/bookmarks"
          element={
            <ProtectedRoute>
              <BookmarksPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/me"
          element={
            <ProtectedRoute>
              <MyPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/me/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/me/password"
          element={
            <ProtectedRoute>
              <PasswordPage />
            </ProtectedRoute>
          }
        />

        <Route path="/dev" element={<DevPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AppShell>
  );
}

export default App;
