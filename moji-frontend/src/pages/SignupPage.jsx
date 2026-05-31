// pages/SignupPage.jsx
//
// 회원가입 폼. Phase 3에서 공통 <Input>/<Button>/<Modal>로 리팩토링.
// 가입 성공 시 alert → Modal로 교체 (SCREEN_SPEC §4-7):
//   "지금 입력하기" → /me/profile, "나중에" → /, 배경/ESC도 "나중에"와 동일
//
// 모달 트릭 (plan §C):
//  signup 성공 후 즉시 login()을 호출하면 isAuthenticated=true가 되어 상단 가드가
//  /로 즉시 리다이렉트시켜 모달이 못 뜸. 그래서 pendingTokens에 보관 후 모달
//  액션에서 비로소 login()+navigate() 실행.

import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { signup as apiSignup } from "../api/auth";
import { useAuth } from "../contexts/AuthContext";
import Input from "../components/common/Input";
import Button from "../components/common/Button";
import Modal from "../components/common/Modal";

/**
 * SignupPage
 *
 * @returns {JSX.Element}
 */
export default function SignupPage() {
  const { isAuthenticated, login } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [formError, setFormError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 가입 성공 후 모달 표시용 — 토큰은 모달 액션 전까지 미반영
  const [pendingTokens, setPendingTokens] = useState(null);

  // 이미 로그인된 상태로 /signup 에 진입하면 홈으로 보냄.
  // 단, pendingTokens 가 살아 있으면(모달 액션 중 login() 호출로 인한 일시 상태) 가드 발동 X —
  // 그 흐름에서는 같은 핸들러의 navigate(target) 가 곧바로 이동시키므로 가드가 끼어들면 안 된다.
  if (isAuthenticated && !pendingTokens) return <Navigate to="/" replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setFieldErrors({});
    setFormError("");

    // 클라이언트 측 비밀번호 일치 검증
    if (password !== passwordConfirm) {
      setFieldErrors({ passwordConfirm: "비밀번호가 일치하지 않아요." });
      return;
    }

    setSubmitting(true);
    try {
      const data = await apiSignup({ email, password });
      // 토큰은 보관만 — 모달 액션 후 login() 호출
      setPendingTokens({
        access: data.accessToken,
        refresh: data.refreshToken,
      });
    } catch (err) {
      if (err.errorCode === "VALIDATION_FAILED") {
        setFieldErrors(err.fields || {});
      } else if (err.errorCode === "EMAIL_ALREADY_EXISTS") {
        setFieldErrors({ email: err.message });
      } else {
        setFormError(err.message);
      }
    } finally {
      setSubmitting(false);
    }
  }

  /** "지금 입력하기" — 프로필 페이지로 */
  function goToProfile() {
    login(pendingTokens.access, pendingTokens.refresh);
    navigate("/me/profile", { replace: true });
  }

  /** "나중에" / 배경 클릭 / ESC — 홈으로 */
  function skipProfile() {
    login(pendingTokens.access, pendingTokens.refresh);
    navigate("/", { replace: true });
  }

  return (
    <>
      <main className="min-h-[80vh] flex items-center justify-center px-4 py-12 bg-gradient-to-br from-brand-subtle via-surface to-accent-subtle">
        <div className="w-full max-w-md">
          <div className="bg-surface rounded-xl-card border border-surface-border shadow-card p-8 md:p-10">
            <div className="flex flex-col items-center mb-6">
              <img
                src="/reference/mozi-mascot.png"
                alt=""
                className="h-28 w-auto object-contain mb-3"
              />
              <h1 className="text-senior-xl font-extrabold text-ink-strong">
                MOZI 회원가입
              </h1>
              <p className="mt-2 text-senior-sm text-ink-weak">
                나에게 꼭 맞는 복지를 만나보세요
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
                autoComplete="new-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                hint="8자 이상 입력해주세요."
                error={fieldErrors.password}
              />
              <Input
                label="비밀번호 확인"
                type="password"
                autoComplete="new-password"
                value={passwordConfirm}
                onChange={(e) => setPasswordConfirm(e.target.value)}
                required
                error={fieldErrors.passwordConfirm}
              />

              <Button type="submit" large disabled={submitting}>
                {submitting ? "가입 중…" : "회원가입"}
              </Button>
            </form>

            <p className="mt-6 text-center text-senior-sm text-ink-weak">
              이미 계정이 있으신가요?{" "}
              <Link to="/login" className="text-brand-deep font-bold underline">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </main>

      <Modal
        open={pendingTokens !== null}
        onClose={skipProfile}
        title="환영해요!"
      >
        <p className="mb-6">
          마이페이지에서 프로필(나이·지역 등)을 입력하시면 더 정확한 복지
          추천을 받을 수 있어요.
        </p>
        <div className="flex gap-2 justify-end">
          <Button variant="secondary" onClick={skipProfile}>
            나중에
          </Button>
          <Button onClick={goToProfile}>지금 입력하기</Button>
        </div>
      </Modal>
    </>
  );
}
