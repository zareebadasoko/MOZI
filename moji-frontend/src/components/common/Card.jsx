// components/common/Card.jsx
//
// 흰 배경 + 옅은 그림자 + 둥근 모서리. STYLING_GUIDE §5-3.
// onClick이 있으면 <button>으로, 없으면 <div>로 렌더 (전체가 버튼처럼 동작).
//
// 사용:
//   <Card>내용</Card>
//   <Card onClick={() => navigate(`/welfares/${id}`)}>클릭 가능 카드</Card>

/**
 * Card
 *
 * @param {Object} props
 * @param {() => void} [props.onClick] - 있으면 전체 카드가 클릭 가능 (button 태그)
 * @param {React.ReactNode} props.children
 * @returns {JSX.Element}
 */
export default function Card({ onClick, children }) {
  const Tag = onClick ? "button" : "div";
  return (
    <Tag
      type={onClick ? "button" : undefined}
      onClick={onClick}
      className={
        "block w-full text-left p-5 rounded-card bg-surface border border-surface-border shadow-card " +
        (onClick ? "hover:shadow-modal transition-shadow cursor-pointer" : "")
      }
    >
      {children}
    </Tag>
  );
}
