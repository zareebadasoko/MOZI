// pages/WelfareSearchPage.jsx
//
// /welfares — 복지 검색 페이지. Optional Auth — 비로그인도 진입 가능.
// useWelfareSearch 훅에서 모든 상태(필터·페이지·결과·로딩·에러)를 받아 화면에 분배.
//
// 화면 구성:
//  - PageTitle "복지 찾기" + 도우미 안내
//  - WelfareFilterBar (좌측 사이드 또는 상단)
//  - 본문: Spinner / ErrorBox / WelfareCardList + Pagination

import WelfareFilterBar from "../components/welfare/WelfareFilterBar";
import WelfareCardList from "../components/welfare/WelfareCardList";
import Pagination from "../components/common/Pagination";
import Spinner from "../components/common/Spinner";
import ErrorBox from "../components/common/ErrorBox";
import { useWelfareSearch } from "../hooks/useWelfareSearch";

/**
 * WelfareSearchPage
 *
 * @returns {JSX.Element}
 */
export default function WelfareSearchPage() {
  const {
    filters,
    setFilter,
    setRegion,
    categories,
    regions,
    results,
    page,
    totalCount,
    totalPages,
    loading,
    error,
    search,
    goToPage,
    reset,
  } = useWelfareSearch();

  const isFirstLoad = loading && results.length === 0;
  const showInitialError = !!error && results.length === 0;

  const handlePageChange = (nextPage) => {
    goToPage(nextPage);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  return (
    <main className="max-w-screen-xl mx-auto px-4 md:px-8 py-10 md:py-14">
      <header className="mb-8">
        <p className="text-senior-sm font-bold text-brand-deep">복지 찾기</p>
        <h1 className="mt-2 text-senior-xl md:text-senior-2xl font-extrabold text-ink-strong">
          조건에 맞는 복지를 찾아드려요
        </h1>
        <p className="mt-3 text-senior-base text-ink-weak max-w-2xl">
          키워드, 카테고리, 지역, 제공 기관을 선택해서 검색해보세요.
        </p>
      </header>

      <div className="grid lg:grid-cols-[360px_1fr] gap-6">
        {/* 필터 */}
        {/* lg 이상에서 sticky 로 viewport 상단 6rem(top-24) 에 고정.
            필터 내용이 viewport 보다 길어질 수 있어 max-h-sticky-side + overflow-y-auto 로
            사이드바 자체에 내부 스크롤을 부여한다. */}
        <aside className="lg:sticky lg:top-24 lg:max-h-sticky-side lg:overflow-y-auto self-start">
          <WelfareFilterBar
            filters={filters}
            categories={categories}
            regions={regions}
            onChange={setFilter}
            onRegionChange={setRegion}
            onSubmit={search}
            onReset={reset}
            disabled={loading}
          />
        </aside>

        {/* 결과 */}
        <section>
          {!isFirstLoad && !showInitialError && results.length > 0 && (
            <p className="text-senior-base text-ink-weak mb-4">
              총 <span className="text-brand-deep font-extrabold">{totalCount}</span>건
            </p>
          )}

          {isFirstLoad ? (
            <Spinner label="복지 정보를 불러오고 있어요" />
          ) : showInitialError ? (
            <ErrorBox
              message="복지 정보를 불러올 수 없어요. 잠시 후 다시 시도해주세요."
              onRetry={search}
            />
          ) : (
            <WelfareCardList
              items={results}
              emptyMessage="조건에 맞는 복지가 없어요. 다른 키워드로 검색해보시겠어요?"
              emptyActionLabel="필터 초기화"
              onEmptyAction={reset}
            />
          )}

          {!isFirstLoad && results.length > 0 && (
            <div className="mt-8">
              <Pagination
                page={page}
                totalPages={totalPages}
                onChange={handlePageChange}
                disabled={loading}
              />
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
