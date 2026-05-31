// components/common/ToastContainer.jsx
//
// ToastContext의 toasts 큐를 화면 하단에 스택으로 렌더한다.
// AppShell 안에 한 번만 둔다.
//
// 위치: 화면 하단 중앙, fixed. 여러 개일 때 위로 쌓임 (gap-2).

import Toast from "./Toast";
import { useToast } from "../../hooks/useToast";

/**
 * ToastContainer
 *
 * AppShell의 최하단에 한 번만 둔다 (전역 레이아웃 일부).
 *
 * @returns {JSX.Element}
 */
export default function ToastContainer() {
  const { toasts, dismissToast } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div
      // pointer-events-none: 컨테이너 자체는 클릭 통과 (뒤 콘텐츠 클릭 가능)
      // 자식 토스트는 pointer-events-auto로 자체 클릭 가능
      className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex flex-col-reverse items-center gap-2 pointer-events-none"
    >
      {toasts.map((t) => (
        <div key={t.id} className="pointer-events-auto">
          <Toast
            kind={t.kind}
            message={t.message}
            onDismiss={() => dismissToast(t.id)}
          />
        </div>
      ))}
    </div>
  );
}
