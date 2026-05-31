// components/chat/RecommendedWelfares.jsx
//
// 챗봇 응답에 딸려오는 추천 복지 카드 묶음. items.length === 0 이면 컴포넌트 자체 null 반환.
// 검색 페이지의 WelfareCard 를 그대로 재사용 — onBookmarkChange 미전달이라
// 카드의 ★ 는 시각 표식만(클릭 동작 없음). 카드 본체 클릭은 상세 페이지로 이동.

import WelfareCard from "../welfare/WelfareCard";

/**
 * RecommendedWelfares
 *
 * @param {Object} props
 * @param {Array} props.items - WelfareSummaryDto 배열
 * @returns {JSX.Element | null}
 */
export default function RecommendedWelfares({ items }) {
  if (!items || items.length === 0) return null;
  return (
    <div className="mt-3 ml-2">
      <p className="text-senior-sm text-ink-weak mb-2">
        이런 복지를 추천드려요
      </p>
      <ul className="flex flex-col gap-3">
        {items.map((welfare) => (
          <li key={welfare.id}>
            <WelfareCard welfare={welfare} />
          </li>
        ))}
      </ul>
    </div>
  );
}
