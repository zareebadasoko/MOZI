// components/common/Spinner.jsx
//
// 회전하는 원 로딩 표시. Tailwind의 animate-spin 사용 (CSS keyframe 자동).
//
// 사용:
//   <Spinner />              // 기본 크기 (32px)
//   <Spinner size="sm" />    // 작은 크기 (20px) — 버튼 내부 등
//   <Spinner label="잠시만요…" />  // 텍스트 동반 표시

/**
 * Spinner
 *
 * @param {Object} props
 * @param {"sm"|"base"} [props.size="base"]
 * @param {string} [props.label] - 동반 표시할 안내 텍스트 (있으면 옆에 노출)
 * @returns {JSX.Element}
 */
export default function Spinner({ size = "base", label }) {
  const sizeClass = {
    sm: "h-5 w-5 border-2",
    base: "h-8 w-8 border-[3px]",
  }[size];

  return (
    <div className="inline-flex items-center gap-3" role="status" aria-live="polite">
      <span
        // border 한 면만 다른 색으로 → 회전 시 끊긴 부분이 돌면서 spinner 효과
        className={`${sizeClass} rounded-full border-surface-border border-t-brand animate-spin`}
        aria-hidden="true"
      />
      {label && (
        <span className="text-senior-base text-ink-weak">{label}</span>
      )}
    </div>
  );
}
