// components/chat/ChatMessageList.jsx
//
// 메시지 배열을 위→아래로 렌더. 응답 대기 중 Spinner, 에러 시 RetryBox.
// 새 메시지/로딩 추가될 때마다 맨 아래로 부드럽게 스크롤.

import { useEffect, useRef } from "react";
import ChatBubble from "./ChatBubble";
import RecommendedWelfares from "./RecommendedWelfares";
import Spinner from "../common/Spinner";
import Button from "../common/Button";

/**
 * @typedef {Object} ChatMessage
 * @property {string} id
 * @property {"user"|"assistant"} role
 * @property {string} text
 * @property {Array} [welfares] - assistant 메시지에만 (없거나 빈 배열일 수 있음)
 *
 * @typedef {{ message: string, lastUserMessage: string }} ChatError
 */

/**
 * ChatMessageList
 *
 * @param {Object} props
 * @param {Array<ChatMessage>} props.messages
 * @param {boolean} props.loading - true 면 마지막에 Spinner + 안내
 * @param {ChatError|null} props.error - 챗봇 호출 실패 시 RetryBox 표시
 * @param {() => void} props.onRetry - 재시도 버튼 핸들러
 * @returns {JSX.Element}
 */
export default function ChatMessageList({ messages, loading, error, onRetry }) {
  const bottomRef = useRef(null);

  // 메시지 수가 늘거나 로딩/에러 상태가 바뀔 때마다 맨 아래로 스크롤.
  // !!error 같은 복합 표현은 deps 에 직접 못 넣어 변수로 추출.
  const hasError = !!error;
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages.length, loading, hasError]);

  return (
    <div className="flex flex-col gap-4 py-4">
      {messages.map((m) => (
        <div key={m.id} className="flex flex-col gap-2">
          <ChatBubble role={m.role} text={m.text} />
          {m.role === "assistant" && m.welfares && (
            <RecommendedWelfares items={m.welfares} />
          )}
        </div>
      ))}

      {loading && (
        <div className="flex justify-start">
          <div className="px-4 py-3 rounded-card bg-surface-muted border border-surface-border">
            <Spinner label="답변을 준비 중이에요" />
          </div>
        </div>
      )}

      {error && !loading && (
        <div
          role="alert"
          className="w-full bg-danger-subtle text-danger rounded-card p-4"
        >
          <p className="text-senior-base mb-3">{error.message}</p>
          <Button variant="secondary" onClick={onRetry}>
            다시 시도
          </Button>
        </div>
      )}

      {/* 스크롤 앵커 */}
      <div ref={bottomRef} aria-hidden="true" />
    </div>
  );
}
