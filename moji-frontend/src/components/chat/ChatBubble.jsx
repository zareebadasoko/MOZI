// components/chat/ChatBubble.jsx
//
// 챗봇 페이지의 메시지 한 줄. assistant 는 MOZI 마스코트 아바타와 함께, user 는 우측 정렬.
// 노년층 UX: text-senior-base(18px) 이상 + 충분한 padding + whitespace-pre-line 으로 줄바꿈 보존.

/**
 * ChatBubble
 *
 * @param {Object} props
 * @param {"user"|"assistant"} props.role
 * @param {string} props.text
 * @returns {JSX.Element}
 */
export default function ChatBubble({ role, text }) {
  const isUser = role === "user";

  if (isUser) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[85%] px-5 py-3 rounded-xl-card text-senior-base whitespace-pre-line bg-brand text-ink-invert shadow-soft">
          {text}
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-start gap-3">
      <img
        src="/reference/mozi-avatar.png"
        alt="MOZI"
        className="h-11 w-11 md:h-12 md:w-12 object-contain shrink-0"
      />
      <div className="max-w-[80%]">
        <p className="text-senior-sm font-bold text-brand-deep mb-1">MOZI</p>
        <div className="px-5 py-3 rounded-xl-card rounded-tl-sm text-senior-base whitespace-pre-line bg-surface text-ink-strong border border-surface-border shadow-soft">
          {text}
        </div>
      </div>
    </div>
  );
}
