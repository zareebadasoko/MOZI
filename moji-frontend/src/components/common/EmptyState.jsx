// components/common/EmptyState.jsx
//
// 리스트가 비어있을 때 표시하는 안내 박스.
// 예: 검색 결과 0건, 북마크 없음, 챗봇 메시지 없음.
//
// 사용:
//   <EmptyState icon="🔍" message="아직 검색 결과가 없어요. 다른 키워드로 시도해보세요." />
//   <EmptyState
//     icon="📌"
//     message="저장한 복지가 없어요."
//     actionLabel="복지 찾으러 가기"
//     onAction={() => navigate("/welfares")}
//   />

import Button from "./Button";

/**
 * EmptyState
 *
 * @param {Object} props
 * @param {string} [props.icon] - 큰 이모지/아이콘 (예: "🔍", "📌")
 * @param {string} props.message
 * @param {string} [props.actionLabel] - 있으면 CTA 버튼 라벨
 * @param {() => void} [props.onAction] - actionLabel과 함께 제공해야 동작
 * @returns {JSX.Element}
 */
export default function EmptyState({ icon, message, actionLabel, onAction }) {
  return (
    <div className="w-full max-w-md mx-auto py-12 text-center">
      {icon && <div className="text-6xl mb-4" aria-hidden="true">{icon}</div>}
      <p className="text-senior-base text-ink-weak mb-6">{message}</p>
      {actionLabel && onAction && (
        <Button onClick={onAction}>{actionLabel}</Button>
      )}
    </div>
  );
}
