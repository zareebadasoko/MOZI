// utils/conversationStore.js
//
// 챗봇 대화 ID 저장소. 챗봇 페이지(Phase 5)에서만 사용한다.
// sessionStorage에 두는 이유: 페이지 이동 시 대화 유지, 탭 닫으면 새 대화로 시작.
// Context까지 가지 않은 이유: 챗봇 페이지 한 곳에서만 쓰므로 직접 접근으로 충분.

const KEY = "mozi_conversation_id";

/**
 * 현재 저장된 conversationId 가져오기 (없으면 null)
 *
 * @returns {string|null}
 */
export function getConversationId() {
  return sessionStorage.getItem(KEY);
}

/**
 * conversationId 저장 (백엔드 응답으로 받은 UUID)
 *
 * @param {string} id
 */
export function setConversationId(id) {
  sessionStorage.setItem(KEY, id);
}

/**
 * conversationId 삭제 (새 대화 시작 / 로그아웃 / 탈퇴)
 */
export function clearConversationId() {
  sessionStorage.removeItem(KEY);
}
