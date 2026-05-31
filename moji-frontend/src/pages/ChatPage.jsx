// pages/ChatPage.jsx
//
// /chat — 챗봇 상담 페이지. Protected.
// flowchart 의 "챗봇 상담 화면" 흐름:
//   질문 유형 선택 → 텍스트 입력 OR 자주 묻는 질문 버튼 → 질문 분석 → 답변 생성 → 복지 상세
//
// 인프라:
//  · conversationStore  — sessionStorage 의 conversationId 보관 (탭 닫으면 새 대화)
//  · chatHistoryStore   — sessionStorage 의 메시지 배열 보관. 마운트 시 복구 + 변경 시 자동 동기화
//  · AuthContext.logout 이 두 스토어 모두 clear → 로그아웃 시 자동 리셋
//  · 챗봇 에러 3종은 자동 재시도 X — 사용자 명시 재시도 버튼

import { useEffect, useRef, useState } from "react";
import { sendChat } from "../api/chat";
import {
  getConversationId,
  setConversationId,
  clearConversationId,
} from "../utils/conversationStore";
import {
  getChatMessages,
  setChatMessages,
  clearChatMessages,
} from "../utils/chatHistoryStore";
import ChatMessageList from "../components/chat/ChatMessageList";
import ChatInput from "../components/chat/ChatInput";
import Button from "../components/common/Button";
import Modal from "../components/common/Modal";
import { useToast } from "../hooks/useToast";

const INITIAL_GREETING = {
  id: "init",
  role: "assistant",
  text: "안녕하세요. 어떤 복지가 필요하신가요? 아래의 자주 묻는 질문을 눌러보시거나, 직접 적어주세요.",
};

// flowchart 의 "자주 묻는 질문" 버튼 세트. 클릭 시 곧바로 메시지로 전송된다.
const QUICK_QUESTIONS = [
  "노인 일자리 알려줘",
  "기초연금 받을 수 있어?",
  "의료비 지원 받고 싶어",
  "주거비 도와주는 복지 있어?",
  "치매 검진 어떻게 받아?",
  "혼자 사는 어르신 지원",
];

/** 단순한 메시지 ID 생성. 키 충돌만 안 나면 됨. */
function nextId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

/**
 * ChatPage
 *
 * @returns {JSX.Element}
 */
export default function ChatPage() {
  const { showToast } = useToast();

  // 마운트 시 sessionStorage 에서 복구. 없으면 초기 인사 1 개로 시작.
  const [messages, setMessages] = useState(
    () => getChatMessages() || [INITIAL_GREETING],
  );

  // messages 가 바뀔 때마다 sessionStorage 와 동기화.
  useEffect(() => {
    setChatMessages(messages);
  }, [messages]);

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [inputError, setInputError] = useState("");
  const [newChatOpen, setNewChatOpen] = useState(false);

  const conversationIdRef = useRef(getConversationId());
  const [hasConversation, setHasConversation] = useState(
    () => !!getConversationId(),
  );

  async function send(message) {
    setLoading(true);
    setError(null);
    setInputError("");
    try {
      const res = await sendChat({
        message,
        conversationId: conversationIdRef.current,
      });
      if (res?.conversationId) {
        conversationIdRef.current = res.conversationId;
        setConversationId(res.conversationId);
        setHasConversation(true);
      }
      setMessages((prev) => [
        ...prev,
        {
          id: nextId("a"),
          role: "assistant",
          text: res?.reply || "",
          welfares: res?.welfares || [],
        },
      ]);
    } catch (err) {
      if (err.errorCode === "VALIDATION_FAILED") {
        setInputError(err.fields?.message || "메시지를 다시 확인해주세요.");
      } else if (
        err.errorCode === "CHATBOT_TIMEOUT" ||
        err.errorCode === "CHATBOT_UNAVAILABLE" ||
        err.errorCode === "CHATBOT_INVALID_RESPONSE" ||
        err.errorCode === "NETWORK_ERROR"
      ) {
        // 사용자가 재시도 버튼으로 마지막 메시지를 다시 보낼 수 있게 보존
        setError({
          message:
            err.message || "지금은 답변이 어려워요. 잠시 후 다시 시도해주세요.",
          lastUserMessage: message,
        });
      } else {
        showToast({
          kind: "error",
          message: err.message || "잠시 후 다시 시도해주세요.",
        });
      }
    } finally {
      setLoading(false);
    }
  }

  function pushUserAndSend(text) {
    setMessages((prev) => [
      ...prev,
      { id: nextId("u"), role: "user", text },
    ]);
    send(text);
  }

  function handleSubmit() {
    const trimmed = input.trim();
    if (!trimmed) {
      setInputError("메시지를 입력해주세요.");
      return;
    }
    if (loading) return;
    setInput("");
    pushUserAndSend(trimmed);
  }

  function handleQuickQuestion(question) {
    if (loading) return;
    pushUserAndSend(question);
  }

  function handleRetry() {
    if (!error?.lastUserMessage || loading) return;
    send(error.lastUserMessage);
  }

  function handleNewChatConfirm() {
    clearConversationId();
    clearChatMessages();
    conversationIdRef.current = null;
    setHasConversation(false);
    setMessages([INITIAL_GREETING]);
    setError(null);
    setInputError("");
    setInput("");
    setNewChatOpen(false);
  }

  return (
    <main className="max-w-screen-lg mx-auto px-4 md:px-6 py-6 md:py-10">
      <div className="bg-surface rounded-xl-card shadow-card border border-surface-border overflow-hidden flex flex-col min-h-[75vh]">
        {/* 헤더 영역 */}
        <header className="px-6 py-5 border-b border-surface-border bg-gradient-to-r from-brand-subtle to-surface flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <img
              src="/reference/mozi-avatar.png"
              alt=""
              className="h-12 w-12 object-contain"
            />
            <div>
              <h1 className="text-senior-lg md:text-senior-xl font-extrabold text-ink-strong">
                MOZI 도우미
              </h1>
              <p className="text-senior-sm text-ink-weak">
                편하게 말씀해 주세요. 도와드릴게요.
              </p>
            </div>
          </div>

          {hasConversation && (
            <Button
              variant="secondary"
              onClick={() => setNewChatOpen(true)}
              disabled={loading}
            >
              새 대화 시작
            </Button>
          )}
        </header>

        {/* 메시지 영역 */}
        <div className="flex-1 overflow-y-auto px-4 md:px-6 bg-surface-muted/40">
          <ChatMessageList
            messages={messages}
            loading={loading}
            error={error}
            onRetry={handleRetry}
          />
        </div>

        {/* 자주 묻는 질문 — 첫 인사 외에 메시지가 없을 때만 노출 */}
        {messages.length <= 1 && (
          <div className="px-4 md:px-6 py-4 bg-surface border-t border-surface-border">
            <p className="text-senior-sm font-bold text-ink-weak mb-3">
              자주 묻는 질문
            </p>
            <div className="flex flex-wrap gap-2">
              {QUICK_QUESTIONS.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => handleQuickQuestion(q)}
                  disabled={loading}
                  className="h-12 px-5 rounded-pill border border-brand-soft bg-brand-subtle text-brand-deep text-senior-base font-medium hover:bg-brand hover:text-ink-invert hover:border-brand transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {q}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* 입력 영역 */}
        <ChatInput
          value={input}
          onChange={(v) => {
            setInput(v);
            if (inputError) setInputError("");
          }}
          onSubmit={handleSubmit}
          disabled={loading}
          error={inputError}
        />
      </div>

      <Modal
        open={newChatOpen}
        onClose={() => setNewChatOpen(false)}
        title="지금 대화를 끝낼까요?"
      >
        <p className="text-senior-base text-ink-strong mb-6">
          새 대화를 시작하면 지금까지의 대화 흐름이 초기화돼요.
        </p>
        <div className="flex flex-col-reverse sm:flex-row gap-2 justify-end">
          <Button variant="secondary" onClick={() => setNewChatOpen(false)}>
            취소
          </Button>
          <Button onClick={handleNewChatConfirm}>새 대화 시작</Button>
        </div>
      </Modal>
    </main>
  );
}
