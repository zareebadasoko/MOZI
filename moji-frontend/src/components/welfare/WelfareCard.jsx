// components/welfare/WelfareCard.jsx
//
// 복지 검색/북마크 결과 1건. 카드 본체 클릭 시 상세 페이지로 이동한다.
//
// 북마크 표식:
//  - onBookmarkChange 미전달(검색 페이지): isBookmarked=true 일 때 시각 ★ 만 표시(클릭 동작 X).
//  - onBookmarkChange 전달(북마크 목록 페이지): ★ 가 클릭 가능한 별도 버튼(절대 위치)으로 노출.
//
// DOM 구조 주의: Card 가 button 요소라 ★ 토글 버튼을 내부에 두면 button-in-button(HTML 위반)
//   → 절대 위치 버튼을 Card 의 형제 노드로 배치하고 stopPropagation 으로 카드 클릭과 분리한다.

import { useLocation, useNavigate } from "react-router-dom";
import Card from "../common/Card";
import { WELFARE_TYPE_LABEL } from "../../constants/welfareType";

/**
 * @typedef {Object} CategorySummary
 * @property {string} code
 * @property {string} name
 * @property {"THEME"|"STATUS"} type
 *
 * @typedef {Object} WelfareSummary
 * @property {string} id
 * @property {string} title
 * @property {string} summary
 * @property {"CENTRAL"|"LOCAL"|"PRIVATE"|"SEOUL"} welfareType
 * @property {string} organizationName
 * @property {Array<CategorySummary>} categories
 * @property {boolean} isBookmarked
 */

/**
 * WelfareCard
 *
 * @param {Object} props
 * @param {WelfareSummary} props.welfare
 * @param {() => void} [props.onBookmarkChange] - 있으면 ★ 가 토글 버튼이 됨(현재 사용처: BookmarksPage 의 해제).
 *   카드 본체 클릭(=상세 이동)과 ★ 클릭은 stopPropagation 으로 분리.
 * @returns {JSX.Element}
 */
export default function WelfareCard({ welfare, onBookmarkChange }) {
  const navigate = useNavigate();
  const location = useLocation();
  const {
    id,
    title,
    summary,
    welfareType,
    organizationName,
    categories,
    isBookmarked,
  } = welfare;

  // 카테고리 칩은 화면 잡음을 줄이기 위해 최대 3개만 노출.
  const visibleCategories = (categories || []).slice(0, 3);

  // 상세 페이지의 "목록으로" 가 새로고침 후에도 검색 URL 로 돌아갈 수 있도록 현재 URL 을 state.from 으로 보관.
  // (브라우저 뒤로가기는 history 가 알아서 처리하지만, 상세에서 새로고침하면 history index 가 리셋되므로 state 가 보완해 줌)
  const detailHref = `/welfares/${encodeURIComponent(id)}`;
  const fromUrl = location.pathname + location.search;

  return (
    <div className="relative">
      <Card onClick={() => navigate(detailHref, { state: { from: fromUrl } })}>
        <div className="flex items-start justify-between gap-3 mb-3">
          <span className="inline-flex items-center px-3 py-1 rounded-pill bg-brand-subtle text-brand-deep text-senior-sm font-bold">
            {WELFARE_TYPE_LABEL[welfareType] || welfareType}
          </span>
          {/* 시각 ★: 토글 버튼이 없을 때만(검색 페이지). 토글 버튼은 아래 절대 위치 */}
          {!onBookmarkChange && isBookmarked && (
            <span
              className="text-brand text-2xl leading-none"
              aria-label="저장된 복지"
            >
              ★
            </span>
          )}
        </div>

        <h3 className="text-senior-lg font-bold text-ink-strong mb-2">
          {title}
        </h3>

        {summary && (
          <p className="text-senior-base text-ink-weak mb-3 line-clamp-2">
            {summary}
          </p>
        )}

        {visibleCategories.length > 0 && (
          <ul className="flex flex-wrap gap-2 mb-3">
            {visibleCategories.map((cat) => (
              <li
                key={cat.code}
                className="px-2 py-1 rounded-pill bg-surface-muted text-ink-weak text-senior-sm"
              >
                {cat.name}
              </li>
            ))}
          </ul>
        )}

        {organizationName && (
          <p className="text-senior-sm text-ink-mute">{organizationName}</p>
        )}
      </Card>

      {/* 토글 버튼: Card 의 형제 노드라 button-in-button 회피.
          stopPropagation 으로 카드의 클릭(상세 이동)과 명확히 분리. */}
      {onBookmarkChange && (
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onBookmarkChange();
          }}
          aria-label="저장 해제"
          className="absolute top-4 right-4 h-12 w-12 rounded-pill bg-surface border border-surface-border text-brand text-2xl leading-none hover:bg-brand-subtle disabled:opacity-50 flex items-center justify-center"
        >
          ★
        </button>
      )}
    </div>
  );
}
