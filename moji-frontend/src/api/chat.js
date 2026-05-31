// api/chat.js
//
// 챗봇 도메인 API. baseFetch 만 거치며 챗봇 서버를 프론트에서 직접 호출하지 않는다.
// (CLAUDE.md §5-1 — 챗봇 서버 직접 호출 금지. 항상 백엔드 /api/chat 만)
//
// 백엔드 ↔ 챗봇 서버의 책임 분리는 backend_project/docs/CHATBOT_API_CONTRACT.md 참고.
// 백엔드 mock 모드(chatbot.mock-enabled=true) 사용 시 챗봇 서버 없이 시연 응답이 옴.

import { baseFetch } from "./client";

/**
 * 챗봇에게 메시지를 보내고 응답을 받는다.
 *
 * conversationId 정책:
 *  - 첫 호출 시 null 또는 생략 → 백엔드가 새 UUID v4 를 발급해 응답에 포함
 *  - 후속 호출 시 이전 응답의 conversationId 를 그대로 재전송 → 챗봇 서버가 컨텍스트 유지
 *  - sessionStorage 보관(utils/conversationStore.js). 탭 닫으면 자동 삭제.
 *
 * 응답:
 *  - reply: 챗봇이 보내는 텍스트
 *  - welfares: 추천 복지 카드 배열 (WelfareSummaryDto[]). 잡담/범위 외 응답이면 빈 배열.
 *  - conversationId: 발급된(또는 그대로 유지된) UUID
 *
 * @param {Object} args
 * @param {string} args.message - 사용자 입력 (1~1000자)
 * @param {string|null} args.conversationId - 첫 호출 시 null
 * @returns {Promise<{ reply: string, welfares: Array, conversationId: string }>}
 * @throws {ApiError}
 *   - CHATBOT_TIMEOUT (504): 백엔드 ↔ 챗봇 서버 8초 타임아웃 초과
 *   - CHATBOT_UNAVAILABLE (503): 챗봇 서버 5xx/연결 실패
 *   - CHATBOT_INVALID_RESPONSE (502): 챗봇 응답 스키마 위반
 *   - VALIDATION_FAILED (400): message 비어있음/너무 김
 *   - NETWORK_ERROR / UNAUTHORIZED / TOKEN_EXPIRED 등
 */
export function sendChat({ message, conversationId }) {
  return baseFetch("/api/chat", {
    method: "POST",
    body: { message, conversationId: conversationId ?? null },
  });
}
