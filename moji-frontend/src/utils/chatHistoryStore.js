// utils/chatHistoryStore.js
//
// 챗봇 메시지 히스토리 저장소. ChatPage 한 곳에서만 사용한다.
// sessionStorage 에 두는 이유: 페이지 이동(상세 페이지 다녀오기·새로고침) 후에도 대화가 보이도록,
// 탭 닫으면 자동 삭제되어 깨끗한 시작 (conversationStore 와 같은 정책).
//
// 보존 기간:
//  · 새 대화 시작 / 로그아웃 / 새 로그인 / 탭 닫기 — 모두 클리어
//  · 이외엔 유지 (페이지 이동·새로고침에도 살아남음)

const KEY = "mozi_chat_messages";

/**
 * 저장된 메시지 배열 가져오기. 없거나 파싱 실패하면 null.
 *
 * @returns {Array | null}
 */
export function getChatMessages() {
  try {
    const raw = sessionStorage.getItem(KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : null;
  } catch {
    // JSON 파싱 실패(다른 형식의 옛 데이터가 남았다면) — 무시하고 새로 시작
    return null;
  }
}

/**
 * 메시지 배열 저장. 직렬화/용량 초과 실패는 silently 무시.
 * 메모리 state 는 그대로 살아있으므로 현재 세션 동작에는 영향 없음.
 *
 * @param {Array} messages
 */
export function setChatMessages(messages) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify(messages));
  } catch {
    // sessionStorage 5MB 한도 초과 등 — 시연 분량으로는 발생하지 않을 케이스의 안전망
  }
}

/**
 * 메시지 히스토리 클리어. 새 대화 시작 / 로그아웃 / 새 로그인 시 호출.
 */
export function clearChatMessages() {
  sessionStorage.removeItem(KEY);
}
