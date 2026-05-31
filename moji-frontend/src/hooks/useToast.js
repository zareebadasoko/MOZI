// hooks/useToast.js
//
// ToastContext의 짧은 진입 훅. 컴포넌트는 항상 이 훅을 통해 토스트를 호출한다.
// 사용 예:
//   const { showToast } = useToast();
//   showToast({ kind: "success", message: "저장됐어요" });

import { useContext } from "react";
import { ToastContext } from "../contexts/ToastContext";

/**
 * useToast
 *
 * @returns {{
 *   toasts: Array<{id:number, kind:"success"|"error"|"info", message:string}>,
 *   showToast: ({ kind, message }: { kind?:string, message:string }) => number,
 *   dismissToast: (id: number) => void
 * }}
 */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within <ToastProvider>");
  return ctx;
}
