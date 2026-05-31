// components/chat/ChatInput.jsx
//
// 챗봇 메시지 입력칸. 다중 라인 textarea + 전송 버튼.
//  · Enter = 전송, Shift+Enter = 줄바꿈
//  · IME 한글 조합 처리: 조합 중인 Enter 는 무시 (e.nativeEvent.isComposing)
//  · disabled 시 textarea + 버튼 모두 비활성 (응답 대기 중 중복 전송 방지)

import Button from "../common/Button";

/**
 * ChatInput
 *
 * @param {Object} props
 * @param {string} props.value
 * @param {(next: string) => void} props.onChange
 * @param {() => void} props.onSubmit - 사용자가 전송 의도 (Enter 또는 버튼)
 * @param {boolean} [props.disabled=false]
 * @param {string} [props.error] - 입력칸 아래 표시 (예: 빈 메시지)
 * @returns {JSX.Element}
 */
export default function ChatInput({
  value,
  onChange,
  onSubmit,
  disabled = false,
  error,
}) {
  function handleKeyDown(e) {
    if (e.key !== "Enter") return;
    // 한글 IME 조합 중 Enter 는 조합 종료 신호로만 처리되어야 함 → 전송 무시
    if (e.nativeEvent.isComposing) return;
    if (e.shiftKey) return; // 줄바꿈 허용
    e.preventDefault();
    if (disabled) return;
    onSubmit();
  }

  return (
    <div className="border-t border-surface-border bg-surface px-4 py-3">
      <label className="block">
        <span className="sr-only">메시지 입력</span>
        <div className="flex items-end gap-2">
          <textarea
            value={value}
            onChange={(e) => onChange(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="메시지를 입력하세요"
            rows={2}
            disabled={disabled}
            className={
              "flex-1 px-4 py-3 rounded-soft border text-senior-base text-ink-strong resize-none " +
              (error
                ? "border-danger"
                : "border-surface-border focus:border-brand") +
              " focus:outline-none focus:ring-2 focus:ring-brand-subtle " +
              "disabled:bg-surface-muted disabled:cursor-not-allowed"
            }
          />
          <Button
            type="button"
            large
            onClick={onSubmit}
            disabled={disabled}
          >
            전송
          </Button>
        </div>
      </label>
      {error && (
        <p className="mt-2 text-senior-sm text-danger" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
