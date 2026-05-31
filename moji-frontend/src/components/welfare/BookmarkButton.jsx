// components/welfare/BookmarkButton.jsx
//
// 상세 페이지 헤더의 북마크 토글 버튼.
//
// 로그인 상태:
//   - "저장됨 ★"(primary)  ↔  "저장하기 ☆"(secondary) 토글
//   - 낙관적 업데이트: 클릭 즉시 UI 토글 → API 호출 → 실패 시 되돌리기
//   - DELETE 가 BOOKMARK_NOT_FOUND 면 사용자에게 노출하지 않고 UI 상태만 동기화 (이미 unbookmarked)
//
// 비로그인 상태:
//   - 처음엔 버튼만 보이고, 클릭하면 인라인 안내 박스가 노출된다.
//   - "로그인하기" → /login?redirect=현재경로. 로그인 성공 후 자동으로 상세 페이지 복귀.

import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import Button from "../common/Button";
import { useAuth } from "../../contexts/AuthContext";
import { useToast } from "../../hooks/useToast";
import { createBookmark, deleteBookmark } from "../../api/bookmark";

/**
 * BookmarkButton
 *
 * @param {Object} props
 * @param {string} props.welfareId
 * @param {boolean} props.initialBookmarked - 마운트 시점의 북마크 여부 (응답 isBookmarked)
 * @param {(bookmarked: boolean) => void} [props.onChange] - 토글 성공/실패 후 부모에게 알림 (선택)
 * @returns {JSX.Element}
 */
export default function BookmarkButton({
  welfareId,
  initialBookmarked,
  onChange,
}) {
  const { isAuthenticated } = useAuth();
  const { showToast } = useToast();
  const location = useLocation();

  const [bookmarked, setBookmarked] = useState(!!initialBookmarked);
  const [submitting, setSubmitting] = useState(false);
  // 비로그인 상태에서 사용자가 처음 클릭한 뒤에만 안내 박스를 노출 (점진적 공개)
  const [showLoginHint, setShowLoginHint] = useState(false);

  // 비로그인 사용자가 로그인 후 자동으로 상세 페이지로 복귀하도록 redirect 쿼리 부착
  const loginHref = `/login?redirect=${encodeURIComponent(
    location.pathname + location.search,
  )}`;

  /**
   * 토글 액션. 낙관적 업데이트 + 실패 시 되돌리기.
   */
  async function handleToggle() {
    if (submitting) return;
    const next = !bookmarked;
    setBookmarked(next);
    setSubmitting(true);
    try {
      if (next) {
        await createBookmark(welfareId);
        showToast({ kind: "success", message: "저장되었어요." });
      } else {
        await deleteBookmark(welfareId);
        showToast({ kind: "success", message: "저장이 해제되었어요." });
      }
      onChange?.(next);
    } catch (err) {
      // 이미 없는 북마크를 다시 삭제한 케이스 — UI 는 이미 unbookmarked 로 토글된 상태라
      // 사용자에게 굳이 알릴 필요 없음 (백엔드와의 상태 정합만 맞추면 됨)
      if (err.errorCode === "BOOKMARK_NOT_FOUND") {
        onChange?.(next);
        return;
      }
      // 그 외 실패는 토글 되돌리기 + 안내
      setBookmarked(!next);
      showToast({
        kind: "error",
        message: err.message || "잠시 후 다시 시도해주세요.",
      });
    } finally {
      setSubmitting(false);
    }
  }

  // 비로그인: 버튼 + (클릭 후) 인라인 안내 박스
  if (!isAuthenticated) {
    return (
      <div className="flex flex-col gap-3">
        <Button
          variant="secondary"
          large
          onClick={() => setShowLoginHint(true)}
        >
          저장하기 ☆
        </Button>
        {showLoginHint && (
          <div
            role="status"
            className="bg-brand-subtle text-ink-strong rounded-card p-4"
          >
            <p className="text-senior-base mb-3">
              저장하려면 로그인이 필요해요.
            </p>
            <Link
              to={loginHref}
              className="inline-flex items-center justify-center h-12 px-6 rounded-soft bg-brand text-ink-invert text-senior-base font-medium hover:bg-brand-hover"
            >
              로그인하기
            </Link>
          </div>
        )}
      </div>
    );
  }

  // 로그인: 토글 버튼
  return (
    <Button
      variant={bookmarked ? "primary" : "secondary"}
      large
      onClick={handleToggle}
      disabled={submitting}
      aria-pressed={bookmarked}
    >
      {bookmarked ? "저장됨 ★" : "저장하기 ☆"}
    </Button>
  );
}
