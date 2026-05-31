// pages/LoginPage.jsx
//
// /login — 로그인 폼.
// ProtectedRoute 가 부착한 ?redirect= 쿼리가 있으면 성공 후 그쪽으로 이동.
//
// 에러 분기:
//  - VALIDATION_FAILED → fields 객체를 입력란 아래에 표시
//  - INVALID_CREDENTIALS → 폼 상단에 표시 (어느 필드가 틀렸는지 노출 안 함)
//  - 기타 → 폼 상단에 message

import { useState } from "react";
import { Link, Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { login as apiLogin } from "../api/auth";
import { useAuth } from "../contexts/AuthContext";
import Input from "../components/common/Input";
import Button from "../components/common/Button";

/**
 * LoginPage
 *
 * @returns {JSX.Element}
 */
export default function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const redirectTarget = searchParams.get("redirect") || "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to={redirectTarget} replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setFieldErrors({});
    setFormError("");
    setSubmitting(true);
    try {
      const tokens = await apiLogin({ email, password });
      login(tokens.accessToken, tokens.refreshToken);
      navigate(redirectTarget, { replace: true });
    } catch (err) {
      if (err.errorCode === "VALIDATION_FAILED") {
        setFieldErrors(err.fields || {});
      } else {
        setFormError(err.message);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-gradient-to-br from-brand-subtle via-surface to-accent-subtle">
      <div className="w-full max-w-md">
        <div className="bg-surface rounded-xl-card border border-surface-border shadow-card p-8 md:p-10">
          <div className="flex flex-col items-center mb-6">
            <img
              src="/reference/mozi-avatar.png"
              alt=""
              className="h-20 w-20 object-contain mb-3"
            />
            <h1 className="text-senior-xl font-extrabold text-ink-strong">
              MOZI 로그인
            </h1>
            <p className="mt-2 text-senior-sm text-ink-weak">
              어르신을 위한 맞춤 복지 도우미
            </p>
          </div>

          {formError && (
            <p
              role="alert"
              className="mb-4 px-4 py-3 rounded-soft bg-danger-subtle text-senior-base text-danger"
            >
              {formError}
            </p>
          )}

          <form
            onSubmit={handleSubmit}
            noValidate
            className="flex flex-col gap-4"
          >
            <Input
              label="이메일"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              error={fieldErrors.email}
            />
            <Input
              label="비밀번호"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              error={fieldErrors.password}
            />

            <Button type="submit" large disabled={submitting}>
              {submitting ? "로그인 중…" : "로그인"}
            </Button>
          </form>

          <p className="mt-6 text-center text-senior-sm text-ink-weak">
            아직 계정이 없으신가요?{" "}
            <Link to="/signup" className="text-brand-deep font-bold underline">
              회원가입
            </Link>
          </p>

          <p className="mt-3 text-center text-senior-sm text-ink-weak">
            로그인 없이{" "}
            <Link to="/welfares" className="text-brand-deep font-bold underline">
              복지 둘러보기
            </Link>
          </p>
        </div>
      </div>
    </main>
  );
}
