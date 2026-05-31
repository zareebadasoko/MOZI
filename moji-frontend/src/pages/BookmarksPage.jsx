// pages/BookmarksPage.jsx
//
// /bookmarks 페이지. Protected — ProtectedRoute 가 비로그인 차단.
// useBookmarks 훅이 URL 동기화·낙관적 해제·자동 prev 페이지 이동까지 책임지므로
// 본 페이지는 상태 분기에 따른 렌더만 담당.

import { useNavigate } from "react-router-dom";
import { useBookmarks } from "../hooks/useBookmarks";
import WelfareCardList from "../components/welfare/WelfareCardList";
import Pagination from "../components/common/Pagination";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";

/**
 * BookmarksPage
 *
 * @returns {JSX.Element}
 */
export default function BookmarksPage() {
  const navigate = useNavigate();
  const {
    items,
    page,
    totalCount,
    totalPages,
    loading,
    error,
    goToPage,
    removeBookmark,
  } = useBookmarks();

  const isFirstLoad = loading && items.length === 0;
  const showInitialError = !!error && items.length === 0;

  // 페이지 번호 클릭 시 부드럽게 상단으로 — 검색 페이지와 동일 패턴
  const handlePageChange = (next) => {
    goToPage(next);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <main className="max-w-screen-xl mx-auto px-4 md:px-8 py-10 md:py-14">
      <header className="mb-8">
        <p className="text-senior-sm font-bold text-brand-deep">마이페이지</p>
        <h1 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
          저장한 복지
        </h1>
      </header>

      {!isFirstLoad && !showInitialError && items.length > 0 && (
        <p className="text-senior-base text-ink-weak mb-4">
          총 <span className="text-brand-deep font-extrabold">{totalCount}</span>건
        </p>
      )}

      <div className="mt-4">
        {isFirstLoad ? (
          <Spinner label="저장한 복지를 불러오고 있어요" />
        ) : showInitialError ? (
          <ErrorBox
            message={
              error.message ||
              "저장한 복지를 불러올 수 없어요. 잠시 후 다시 시도해주세요."
            }
            onRetry={() => goToPage(page)}
          />
        ) : (
          <WelfareCardList
            items={items}
            emptyIcon="🔖"
            emptyMessage="아직 저장한 복지가 없어요."
            emptyActionLabel="복지 찾으러 가기"
            onEmptyAction={() => navigate("/welfares")}
            onBookmarkChange={(welfare) => removeBookmark(welfare.id)}
          />
        )}
      </div>

      {!isFirstLoad && items.length > 0 && (
        <div className="mt-6">
          <Pagination
            page={page}
            totalPages={totalPages}
            onChange={handlePageChange}
            disabled={loading}
          />
        </div>
      )}
    </main>
  );
}
