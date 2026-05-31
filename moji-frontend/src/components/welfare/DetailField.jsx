// components/welfare/DetailField.jsx
//
// 상세 페이지에서 "라벨 + 값"을 한 행으로 표시하는 작은 공통 컴포넌트.
// 백엔드의 LONGTEXT 필드는 줄바꿈(\n)을 포함할 수 있어 whitespace-pre-line 으로 보존한다.
// 값이 empty(null/undefined/공백/빈 배열) 면 컴포넌트 자체가 null 을 반환 → 빈 행이 그려지지 않음.
//
// 사용:
//   <DetailField label="지원 대상">{welfare.targetAudience}</DetailField>
//   <DetailField label="문의 전화">{detail.contactNumber}</DetailField>

/**
 * 값이 사실상 비어있는지 판단.
 * 문자열이면 trim 후 길이로, 배열이면 길이로, 그 외엔 truthy 체크.
 */
function isEmpty(value) {
  if (value === null || value === undefined) return true;
  if (typeof value === "string") return value.trim().length === 0;
  if (Array.isArray(value)) return value.length === 0;
  return false;
}

/**
 * DetailField
 *
 * @param {Object} props
 * @param {string} props.label - 행 라벨 (예: "지원 대상")
 * @param {React.ReactNode} props.children - 표시할 값. empty면 컴포넌트는 렌더 안 함.
 * @returns {JSX.Element|null}
 */
export default function DetailField({ label, children }) {
  if (isEmpty(children)) return null;

  return (
    <div className="py-3 border-b border-surface-border last:border-b-0">
      <dt className="text-senior-sm text-ink-weak mb-1">{label}</dt>
      <dd className="text-senior-base text-ink-strong whitespace-pre-line">
        {children}
      </dd>
    </div>
  );
}
