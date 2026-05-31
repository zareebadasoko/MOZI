package com.mozi.backend.domain.chat.client.dto;

import java.util.List;

/**
 * 챗봇 서버 → 백엔드 응답의 Response 페이로드 (외부 API 계약).
 *
 * `recommendedWelfareIds`는 우리 DB의 `welfare_common.id` 자연키 형식(WLF/BOK/SEL prefix).
 * 챗봇 서버가 우리 DB에 존재하지 않는 ID를 반환하면 백엔드가 자동으로 응답에서 제외하고 WARN 로그
 * (`docs/CHATBOT_API_CONTRACT.md` §4-2 동작 명시).
 *
 * @param reply 사용자에게 보여줄 답변 텍스트
 * @param conversationId Request의 conversationId 반환값 (백엔드 발급한 ID 그대로 또는 검증용)
 * @param recommendedWelfareIds 추천 복지 자연키 목록 (없으면 빈 배열, 순서 유지)
 */
public record ChatbotResponse(
        String reply,
        String conversationId,
        List<String> recommendedWelfareIds
) {
}
// 이 record의 역할: 챗봇 서버 응답의 외부 Response 페이로드.
// 챗봇이 새 conversationId를 발급하지 않도록 docs/CHATBOT_API_CONTRACT §4-4에서 합의된 정책.
