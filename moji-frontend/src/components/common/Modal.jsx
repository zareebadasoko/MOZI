// components/common/Modal.jsx
//
// 화면 중앙에 카드 형태로 띄우고 배경을 어둡게. STYLING_GUIDE §5-4 + 보강.
// 보강 사항:
//  - ESC 키로 닫기 (검증 #3)
//  - 모달 외부(배경) 클릭으로 닫기 (검증 #3)
//  - body scroll lock은 Phase 6에서 추가 예정 (지금은 생략)
//  - focus trap도 Phase 6 예정 (접근성 보강)
//
// 사용:
//   const [open, setOpen] = useState(false);
//   <Modal open={open} onClose={() => setOpen(false)} title="확인">
//     <p>정말 탈퇴하시겠어요?</p>
//   </Modal>

import { useEffect } from "react";

/**
 * Modal
 *
 * @param {Object} props
 * @param {boolean} props.open
 * @param {() => void} props.onClose - 배경/ESC/닫기 버튼 트리거
 * @param {string} props.title
 * @param {React.ReactNode} props.children
 * @returns {JSX.Element|null}
 */
export default function Modal({ open, onClose, title, children }) {
  // ESC 키로 닫기. open이 false면 리스너 등록 안 함 (불필요한 이벤트 처리 회피).
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md bg-surface rounded-card shadow-modal p-6"
        onClick={(e) => e.stopPropagation()} // 모달 내부 클릭은 닫기 트리거 안 함
      >
        <h2
          id="modal-title"
          className="text-senior-lg font-bold text-ink-strong mb-4"
        >
          {title}
        </h2>
        <div className="text-senior-base text-ink">{children}</div>
      </div>
    </div>
  );
}
