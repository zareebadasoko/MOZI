// components/common/Toast.jsx
//
// 단일 토스트 UI. ToastContainer가 큐를 받아 이 컴포넌트로 매핑한다.
// STYLING_GUIDE §5-5 패턴.

/**
 * Toast
 *
 * @param {Object} props
 * @param {"success"|"error"|"info"} [props.kind="info"]
 * @param {string} props.message
 * @param {() => void} [props.onDismiss] - 토스트 우측 닫기(X) 버튼 클릭 시 호출
 * @returns {JSX.Element}
 */
export default function Toast({ kind = "info", message, onDismiss }) {
  const color = {
    success: "bg-success text-ink-invert",
    error: "bg-danger text-ink-invert",
    info: "bg-ink-strong text-ink-invert",
  }[kind];

  return (
    <div
      role="status"
      className={`px-6 h-12 flex items-center gap-3 rounded-pill shadow-modal text-senior-base ${color}`}
    >
      <span>{message}</span>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          aria-label="알림 닫기"
          className="ml-1 text-senior-base hover:opacity-80"
        >
          ✕
        </button>
      )}
    </div>
  );
}
