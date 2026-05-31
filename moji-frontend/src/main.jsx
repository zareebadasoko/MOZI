import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import App from "./App.jsx";
import { AuthProvider } from "./contexts/AuthContext";
import { ToastProvider } from "./contexts/ToastContext";

// Provider 중첩 순서:
//  StrictMode > BrowserRouter > AuthProvider > ToastProvider > App
//  - BrowserRouter: useNavigate/<Navigate>가 동작하려면 가장 바깥 (라우팅이 다른 모든 것의 기반)
//  - AuthProvider: 도메인 컨텍스트 (Auth 상태)
//  - ToastProvider: UI 컨텍스트 (Auth와 독립이지만 관례상 도메인 다음)
createRoot(document.getElementById("root")).render(
  <StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <App />
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  </StrictMode>,
);
