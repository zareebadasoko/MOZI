// components/common/ErrorBox.jsx
//
// 에러 메시지 + 선택적 재시도 버튼을 담은 박스.
// API 호출 실패 시 페이지 본문 자리에 표시 (Phase 4 검색 실패, 상세 페이지 로드 실패 등).
//
// 사용:
//   <ErrorBox message="복지 정보를 불러올 수 없어요." onRetry={() => refetch()} />

import Button from "./Button";

/**
 * ErrorBox
 *
 * @param {Object} props
 * @param {string} props.message - 사용자에게 보일 에러 메시지 (한국어, 기술 용어 X)
 * @param {() => void} [props.onRetry] - 있으면 "다시 시도" 버튼 표시
 * @returns {JSX.Element}
 */
export default function ErrorBox({ message, onRetry }) {
  return (
    <div
      role="alert"
      className="w-full max-w-md mx-auto bg-danger-subtle text-danger rounded-card p-6 text-center"
    >
      <p className="text-senior-base mb-4">{message}</p>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          다시 시도
        </Button>
      )}
    </div>
  );
}
