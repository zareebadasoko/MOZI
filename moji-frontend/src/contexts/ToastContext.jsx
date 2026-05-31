// contexts/ToastContext.jsx
//
// 화면 하단에 짧게 떴다 사라지는 토스트 알림의 전역 상태.
//
// 큐 정책 (COMPONENT_ARCHITECTURE §7 + 노년층 UX 보강):
//  - 동시 표시 최대 3개. 초과 시 가장 오래된 토스트 즉시 제거 (FIFO)
//  - 4초 후 자동 소멸 (노년층 UX: 4초 이상 — STYLING_GUIDE §10)
//  - 각 토스트는 자기 setTimeout을 가짐. 수동 dismiss 시 clearTimeout으로 정리.
//
// Provider 언마운트 시 cleanup으로 남은 타이머 정리 (메모리 누수 방지).

import { createContext, useCallback, useEffect, useRef, useState } from "react";

export const ToastContext = createContext(null);

const MAX_TOASTS = 3;
const AUTO_DISMISS_MS = 4000;

// 모듈 변수로 ID 카운터 — 컴포넌트가 리렌더돼도 항상 새 ID 보장.
// crypto.randomUUID() 대안도 있지만 카운터가 더 단순하고 충돌 없음.
let nextId = 0;

/**
 * ToastProvider
 *
 * 앱 최상단(AuthProvider 안)에서 한 번 감싼다.
 *
 * @param {Object} props
 * @param {React.ReactNode} props.children
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  // 활성 토스트의 setTimeout ID를 id별로 보관 (수동 dismiss 시 clearTimeout 위함).
  // useRef를 쓰는 이유: 변경돼도 리렌더 트리거하지 않음 (순수 사이드 채널).
  const timersRef = useRef(new Map());

  /**
   * 토스트를 큐에서 제거하고 해당 타이머도 정리.
   * useCallback으로 안정적인 참조 유지 — 자식 컴포넌트의 리렌더 줄임.
   */
  const dismissToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
    const timer = timersRef.current.get(id);
    if (timer) {
      clearTimeout(timer);
      timersRef.current.delete(id);
    }
  }, []);

  /**
   * 새 토스트 추가.
   *
   * @param {Object} args
   * @param {"success"|"error"|"info"} [args.kind="info"]
   * @param {string} args.message
   * @returns {number} 발급된 토스트 ID (수동 dismiss할 때 사용)
   */
  const showToast = useCallback(
    ({ kind = "info", message }) => {
      const id = ++nextId;

      setToasts((prev) => {
        const next = [...prev, { id, kind, message }];
        // 동시 최대 3개 — 초과분(가장 오래된 것)을 정리
        while (next.length > MAX_TOASTS) {
          const removed = next.shift();
          const t = timersRef.current.get(removed.id);
          if (t) {
            clearTimeout(t);
            timersRef.current.delete(removed.id);
          }
        }
        return next;
      });

      // 4초 후 자동 소멸 타이머 등록
      const timer = setTimeout(() => dismissToast(id), AUTO_DISMISS_MS);
      timersRef.current.set(id, timer);

      return id;
    },
    [dismissToast],
  );

  // Provider 언마운트 시 모든 활성 타이머 정리.
  // StrictMode 더블 마운트 대비 — 첫 unmount 시 타이머 다 정리 후 재마운트 → 새 상태 시작.
  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      timers.forEach((t) => clearTimeout(t));
      timers.clear();
    };
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, showToast, dismissToast }}>
      {children}
    </ToastContext.Provider>
  );
}
