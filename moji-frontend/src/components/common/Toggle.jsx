// components/common/Toggle.jsx
//
// ON/OFF 슬라이드 토글. 라벨(필수) + 선택적 hint 를 좌측, 스위치를 우측에 둔다.
// 노년층 UX: 전체 영역(라벨/힌트/스위치)이 큰 클릭 타깃이 되도록 label 로 감싸고
// 시각적으로 충분한 패딩·h-12 이상 확보.
//
// SCREEN_SPEC §10-4: "체크박스보다 슬라이드 토글 권장".
//
// 사용:
//   <Toggle
//     label="장애 등록"
//     hint="등록된 장애가 있으신 경우 켜주세요"
//     checked={isDisabled}
//     onChange={(next) => setForm(p => ({ ...p, isDisabled: next }))}
//   />

/**
 * Toggle
 *
 * @param {Object} props
 * @param {string} props.label - 라벨 (필수, 노년층 UX)
 * @param {boolean} props.checked
 * @param {(next: boolean) => void} props.onChange
 * @param {string} [props.hint] - 라벨 아래 도움말 한 줄
 * @param {boolean} [props.disabled=false]
 * @returns {JSX.Element}
 */
export default function Toggle({
  label,
  checked,
  onChange,
  hint,
  disabled = false,
}) {
  // <label> 로 감싸 라벨/힌트 영역도 함께 클릭 가능 (히트 영역 확대 = 노년층 친화)
  return (
    <label
      className={
        "flex items-center justify-between gap-3 p-4 rounded-card border border-surface-border bg-surface " +
        (disabled
          ? "opacity-50 cursor-not-allowed"
          : "cursor-pointer hover:bg-surface-muted")
      }
    >
      <div className="flex-1 min-w-0">
        <div className="text-senior-base text-ink-strong">{label}</div>
        {hint && (
          <div className="mt-1 text-senior-sm text-ink-weak">{hint}</div>
        )}
      </div>

      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={label}
        disabled={disabled}
        onClick={() => onChange(!checked)}
        className={
          "shrink-0 inline-flex items-center w-14 h-8 rounded-pill p-1 transition-colors " +
          (checked ? "bg-brand" : "bg-surface-border") +
          " disabled:cursor-not-allowed"
        }
      >
        <span
          className={
            "block h-6 w-6 rounded-full bg-surface shadow-card transform transition-transform " +
            (checked ? "translate-x-6" : "translate-x-0")
          }
          aria-hidden="true"
        />
      </button>
    </label>
  );
}
