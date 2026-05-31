// components/common/Button.jsx
//
// 1차/2차/위험 3종 + 강조 CTA(large) 옵션. STYLING_GUIDE §5-1.
//
// 사용:
//   <Button onClick={...}>로그인</Button>
//   <Button variant="secondary">취소</Button>
//   <Button variant="danger" large>회원 탈퇴</Button>
//   <Button type="submit" disabled={submitting}>제출</Button>

/**
 * Button
 *
 * @param {Object} props
 * @param {"primary"|"secondary"|"danger"} [props.variant="primary"]
 * @param {boolean} [props.large=false] - 강조 CTA(높이 56px)
 * @param {React.ReactNode} props.children
 * @param {...any} rest - button 기본 props (onClick, type, disabled 등)
 * @returns {JSX.Element}
 */
export default function Button({
  variant = "primary",
  large = false,
  children,
  ...rest
}) {
  const base =
    "inline-flex items-center justify-center rounded-soft text-senior-base font-medium " +
    "transition-colors disabled:opacity-50 disabled:cursor-not-allowed " +
    (large ? "h-14 px-8" : "h-12 px-6");

  const variantClass = {
    primary: "bg-brand text-ink-invert hover:bg-brand-hover",
    secondary:
      "bg-surface text-ink-strong border border-surface-border hover:bg-surface-muted",
    danger: "bg-danger text-ink-invert hover:bg-danger-hover",
  }[variant];

  // default type="button" — 폼 안에서 호출자가 type 을 명시하지 않은 경우 의도치 않은 submit 방지.
  // 호출자가 명시적으로 type="submit" 을 넘기면 {...rest} 가 덮어쓰므로 그대로 작동.
  return (
    <button type="button" className={`${base} ${variantClass}`} {...rest}>
      {children}
    </button>
  );
}
