// components/common/Pagination.jsx
//
// 이전/다음 화살표 + 현재 페이지 주변 최대 5개 번호. 검색 결과·북마크 등 페이지네이션 공통 컴포넌트.
//
// 노년층 UX 고려:
//  - 모든 버튼 h-12(48px) 이상으로 터치 타깃 확보
//  - text-senior-base(18px) 폰트
//  - 현재 페이지 강조 (bg-brand) + aria-current="page"
//  - 페이지 1개 이하면 컴포넌트가 알아서 숨김 (호출처에서 분기 불필요)
//
// 사용:
//   <Pagination page={page} totalPages={totalPages} onChange={goToPage} disabled={loading} />

/**
 * 현재 페이지 주변에 최대 windowSize 개를 보여주는 0-based 페이지 번호 배열을 만든다.
 * 가장자리에서는 window가 끝쪽으로 밀려도 항상 windowSize 만큼 노출(가능한 한).
 */
function visiblePages(current, total, windowSize = 5) {
  if (total <= windowSize) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const half = Math.floor(windowSize / 2);
  const start = Math.max(0, Math.min(current - half, total - windowSize));
  return Array.from({ length: windowSize }, (_, i) => start + i);
}

/**
 * Pagination
 *
 * @param {Object} props
 * @param {number} props.page - 현재 페이지 (0-based)
 * @param {number} props.totalPages - 전체 페이지 수. 0 또는 1이면 컴포넌트는 렌더하지 않음.
 * @param {(nextPage: number) => void} props.onChange - 사용자가 페이지를 누르면 호출 (0-based)
 * @param {boolean} [props.disabled=false] - 로딩 중 일괄 비활성화
 * @returns {JSX.Element | null}
 */
export default function Pagination({
  page,
  totalPages,
  onChange,
  disabled = false,
}) {
  if (!totalPages || totalPages <= 1) return null;

  const pages = visiblePages(page, totalPages);
  const isFirst = page <= 0;
  const isLast = page >= totalPages - 1;

  return (
    <nav
      aria-label="페이지 이동"
      className="flex flex-wrap items-center justify-center gap-2"
    >
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={disabled || isFirst}
        className={
          "h-12 px-4 rounded-soft border border-surface-border bg-surface text-senior-base text-ink-strong " +
          "hover:bg-surface-muted disabled:opacity-50 disabled:cursor-not-allowed"
        }
      >
        ← 이전
      </button>

      {pages.map((p) => {
        const active = p === page;
        return (
          <button
            key={p}
            type="button"
            onClick={() => onChange(p)}
            disabled={disabled}
            aria-current={active ? "page" : undefined}
            className={
              "min-w-12 h-12 px-3 rounded-soft border text-senior-base " +
              (active
                ? "bg-brand text-ink-invert border-brand"
                : "bg-surface text-ink-strong border-surface-border hover:bg-surface-muted") +
              " disabled:opacity-50 disabled:cursor-not-allowed"
            }
          >
            {p + 1}
          </button>
        );
      })}

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={disabled || isLast}
        className={
          "h-12 px-4 rounded-soft border border-surface-border bg-surface text-senior-base text-ink-strong " +
          "hover:bg-surface-muted disabled:opacity-50 disabled:cursor-not-allowed"
        }
      >
        다음 →
      </button>
    </nav>
  );
}
