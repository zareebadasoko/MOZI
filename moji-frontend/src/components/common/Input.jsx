// components/common/Input.jsx
//
// 라벨(필수) + hint + error 메시지를 한 단위로 묶은 입력란. STYLING_GUIDE §5-2.
// 노년층 UX 강제: placeholder만으로 의미 전달 금지 → label 필수 prop.
//
// 사용:
//   <Input
//     label="이메일"
//     hint="예: hong@example.com"
//     type="email"
//     value={email}
//     onChange={(e) => setEmail(e.target.value)}
//     error={fieldErrors.email}
//   />

/**
 * Input
 *
 * @param {Object} props
 * @param {string} props.label - 입력란 위에 항상 표시되는 라벨 (필수)
 * @param {string} [props.hint] - 입력란 아래의 도움말 (예시값 등). error가 있으면 표시 안 됨.
 * @param {string} [props.error] - 에러 메시지 (있으면 빨간 테두리 + 메시지 표시)
 * @param {...any} rest - input 기본 props (value, onChange, type, autoComplete 등)
 * @returns {JSX.Element}
 */
export default function Input({ label, hint, error, ...rest }) {
  return (
    <label className="block">
      <span className="block text-senior-base text-ink-strong mb-2">{label}</span>
      <input
        className={
          "block w-full h-12 px-4 text-senior-base text-ink-strong rounded-soft border " +
          (error
            ? "border-danger"
            : "border-surface-border focus:border-brand") +
          " focus:outline-none focus:ring-2 focus:ring-brand-subtle"
        }
        {...rest}
      />
      {hint && !error && (
        <span className="block mt-2 text-senior-sm text-ink-weak">{hint}</span>
      )}
      {error && (
        <span className="block mt-2 text-senior-sm text-danger">{error}</span>
      )}
    </label>
  );
}
