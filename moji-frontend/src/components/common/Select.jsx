// components/common/Select.jsx
//
// 라벨(필수) + hint + error 를 한 단위로 묶은 드롭다운. Input.jsx와 동일한 시각·접근성 패턴.
// 노년층 UX: placeholder만으로 의미 전달 금지 → label 필수, 최소 높이 h-12(48px), text-senior-base.
//
// 사용:
//   <Select
//     label="카테고리"
//     value={category}
//     onChange={(e) => setCategory(e.target.value)}
//     options={[
//       { value: "", label: "전체" },
//       { value: "THM001", label: "서민금융" },
//       { value: "THM005", label: "일자리" },
//     ]}
//     error={fieldErrors.category}
//   />
//
// Phase 4-4의 시도/시군구 cascading select 는 본 컴포넌트를 wrap 하거나
// 옵션을 외부에서 잘라 넘기는 방식으로 재사용한다.

/**
 * @typedef {Object} SelectOption
 * @property {string} value
 * @property {string} label
 * @property {boolean} [disabled]
 */

/**
 * Select
 *
 * @param {Object} props
 * @param {string} props.label - 입력란 위에 항상 표시되는 라벨 (필수)
 * @param {Array<SelectOption>} props.options - 옵션 배열. value=""는 "선택 안 함/전체" 의미로 사용 가능.
 * @param {string} [props.hint] - 도움말. error가 있으면 표시 안 됨.
 * @param {string} [props.error] - 에러 메시지 (있으면 빨간 테두리 + 메시지 표시)
 * @param {...any} rest - select 기본 props (value, onChange, name, disabled 등)
 * @returns {JSX.Element}
 */
export default function Select({ label, options = [], hint, error, ...rest }) {
  return (
    <label className="block">
      <span className="block text-senior-base text-ink-strong mb-2">
        {label}
      </span>
      <select
        className={
          "block w-full h-12 px-4 text-senior-base text-ink-strong bg-surface rounded-soft border " +
          (error
            ? "border-danger"
            : "border-surface-border focus:border-brand") +
          " focus:outline-none focus:ring-2 focus:ring-brand-subtle"
        }
        {...rest}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value} disabled={opt.disabled}>
            {opt.label}
          </option>
        ))}
      </select>
      {hint && !error && (
        <span className="block mt-2 text-senior-sm text-ink-weak">{hint}</span>
      )}
      {error && (
        <span className="block mt-2 text-senior-sm text-danger">{error}</span>
      )}
    </label>
  );
}
