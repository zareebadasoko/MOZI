// components/welfare/WelfareCardList.jsx
//
// WelfareCard 배열 + 결과가 0건일 때의 빈 상태.
// 검색 페이지 / 북마크 페이지(Phase 4-3)에서 공통 사용.

import WelfareCard from "./WelfareCard";
import EmptyState from "../common/EmptyState";

/**
 * WelfareCardList
 *
 * @param {Object} props
 * @param {Array<Object>} props.items - WelfareSummary 배열
 * @param {string} [props.emptyMessage="조건에 맞는 복지가 없어요. 다른 키워드로 검색해보시겠어요?"]
 * @param {string} [props.emptyIcon="🔍"]
 * @param {string} [props.emptyActionLabel] - 빈 상태 CTA 라벨 (예: "필터 초기화")
 * @param {() => void} [props.onEmptyAction]
 * @param {(welfare: Object) => void} [props.onBookmarkChange] - 있으면 각 카드의 ★ 가 토글 버튼이 됨
 *   (BookmarksPage 의 해제 동작 등). 검색 페이지는 이 prop 을 안 넘기므로 동작 무영향.
 * @returns {JSX.Element}
 */
export default function WelfareCardList({
  items,
  emptyMessage = "조건에 맞는 복지가 없어요. 다른 키워드로 검색해보시겠어요?",
  emptyIcon = "🔍",
  emptyActionLabel,
  onEmptyAction,
  onBookmarkChange,
}) {
  if (!items || items.length === 0) {
    return (
      <EmptyState
        icon={emptyIcon}
        message={emptyMessage}
        actionLabel={emptyActionLabel}
        onAction={onEmptyAction}
      />
    );
  }

  return (
    <ul className="flex flex-col gap-4">
      {items.map((welfare) => (
        <li key={welfare.id}>
          <WelfareCard
            welfare={welfare}
            onBookmarkChange={
              onBookmarkChange ? () => onBookmarkChange(welfare) : undefined
            }
          />
        </li>
      ))}
    </ul>
  );
}
