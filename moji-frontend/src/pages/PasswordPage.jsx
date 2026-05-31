// pages/PasswordPage.jsx
//
// /me/password — 비밀번호 변경. Protected.
//
// 성공 시 정책 (CLAUDE.md §4-5):
//  - 백엔드가 본 user 의 모든 refreshToken row 삭제 → 다른 기기 강제 재로그인
//  - access 토큰은 stateless JWT 라 서버 무효화 불가 → 프론트가 즉시 clearTokens + /login
//
// 클라이언트 사전 검증으로 서버 호출 전 명백한 입력 오류를 잡고, 서버 에러는 필드 매핑.
// 비밀번호 입력칸의 "보기/숨기기" 토글로 노년층 오타 방지(SCREEN_SPEC §11-8).

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Input from "../components/common/Input";
import Button from "../components/common/Button";
import Toggle from "../components/common/Toggle";
import { useAuth } from "../contexts/AuthContext";
import { useToast } from "../hooks/useToast";
import { changePassword } from "../api/user";

/**
 * 클라이언트 사전 검증. 통과 시 빈 객체 {}, 실패 시 fieldErrors 매핑.
 * 서버 호출은 통과한 경우에만 진행.
 */
function validate({ currentPassword, newPassword, confirmNewPassword }) {
  const errors = {};
  if (!currentPassword) {
    errors.currentPassword = "현재 비밀번호를 입력해주세요.";
  }
  if (!newPassword) {
    errors.newPassword = "새 비밀번호를 입력해주세요.";
  } else if (newPassword.length < 8 || newPassword.length > 72) {
    errors.newPassword = "8자 이상 72자 이하로 입력해주세요.";
  } else if (newPassword === currentPassword) {
    errors.newPassword = "현재 비밀번호와 다른 값을 입력해주세요.";
  }
  if (!confirmNewPassword) {
    errors.confirmNewPassword = "새 비밀번호를 한 번 더 입력해주세요.";
  } else if (newPassword && newPassword !== confirmNewPassword) {
    errors.confirmNewPassword = "새 비밀번호가 서로 달라요.";
  }
  return errors;
}

/**
 * PasswordPage
 *
 * @returns {JSX.Element}
 */
export default function PasswordPage() {
  const navigate = useNavigate();
  const { logout: contextLogout } = useAuth();
  const { showToast } = useToast();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return;
    const errors = validate({
      currentPassword,
      newPassword,
      confirmNewPassword,
    });
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }
    setFieldErrors({});
    setSubmitting(true);
    try {
      await changePassword({ currentPassword, newPassword });
      // 정책상 백엔드가 모든 refreshToken 무효화. 본 기기도 즉시 토큰 클리어 + 로그인 페이지로.
      // 재로그인 후 비밀번호 페이지로 자동 복귀하는 경합은 AuthContext.logout 의
      // intentionalLogout 플래그로 ProtectedRoute 단에서 차단됨 (ProtectedRoute.jsx 참조).
      navigate("/login", { replace: true });
      contextLogout();
      showToast({
        kind: "success",
        message: "비밀번호가 변경되었어요. 다시 로그인해주세요.",
      });
    } catch (err) {
      setSubmitting(false);
      if (err.errorCode === "PASSWORD_MISMATCH") {
        setFieldErrors({ currentPassword: err.message });
      } else if (err.errorCode === "SAME_PASSWORD") {
        setFieldErrors({ newPassword: err.message });
      } else if (err.errorCode === "VALIDATION_FAILED") {
        setFieldErrors(err.fields || {});
      } else {
        showToast({
          kind: "error",
          message: err.message || "잠시 후 다시 시도해주세요.",
        });
      }
    }
  }

  const inputType = showPassword ? "text" : "password";

  return (
    <main className="max-w-screen-md mx-auto px-4 py-6">
      <div className="mb-4">
        <button
          type="button"
          onClick={() => navigate("/me")}
          className="inline-flex items-center h-12 text-senior-base text-ink-weak hover:text-ink-strong"
        >
          ← 마이페이지
        </button>
      </div>

      <h1 className="text-senior-xl font-bold text-ink-strong mb-4">
        비밀번호 변경
      </h1>

      <div
        role="status"
        className="mb-6 p-4 rounded-card bg-warning-subtle text-ink-strong"
      >
        <p className="text-senior-base">
          변경 후 본 기기를 포함한 모든 기기에서 다시 로그인해야 해요.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <Toggle
          label="비밀번호 보기"
          hint="입력 중인 모든 비밀번호를 글자로 보여줍니다."
          checked={showPassword}
          onChange={setShowPassword}
        />

        <Input
          label="현재 비밀번호"
          type={inputType}
          autoComplete="current-password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          error={fieldErrors.currentPassword}
          disabled={submitting}
        />

        <Input
          label="새 비밀번호"
          type={inputType}
          autoComplete="new-password"
          hint="8자 이상 72자 이하로 입력해주세요."
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          error={fieldErrors.newPassword}
          disabled={submitting}
        />

        <Input
          label="새 비밀번호 확인"
          type={inputType}
          autoComplete="new-password"
          value={confirmNewPassword}
          onChange={(e) => setConfirmNewPassword(e.target.value)}
          error={fieldErrors.confirmNewPassword}
          disabled={submitting}
        />

        <div className="flex flex-col-reverse sm:flex-row gap-2 justify-end mt-2">
          <Button
            variant="secondary"
            type="button"
            onClick={() => navigate("/me")}
            disabled={submitting}
          >
            취소
          </Button>
          <Button type="submit" large disabled={submitting}>
            {submitting ? "변경 중…" : "변경하기"}
          </Button>
        </div>
      </form>
    </main>
  );
}
