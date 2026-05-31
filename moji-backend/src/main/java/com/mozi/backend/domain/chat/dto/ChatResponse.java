package com.mozi.backend.domain.chat.dto;

import com.mozi.backend.domain.welfare.dto.WelfareSummaryDto;

import java.util.List;

/**
 * POST /api/chat 응답 DTO (백엔드 → 클라이언트).
 *
 * `welfares`는 Phase 4-2 검색 응답의 단위 항목인 `WelfareSummaryDto`를 그대로 재사용해
 * 프론트가 동일 카드 컴포넌트로 렌더링 가능. 챗봇이 추천한 ID 순서를 그대로 유지하며,
 * 응답에 `categories`와 `isBookmarked`까지 포함되어 검색 결과와 동등한 UX 제공.
 *
 * `conversationId`는 항상 응답에 포함 — 첫 요청 시 백엔드가 발급한 새 ID 또는
 * 후속 요청 시 그대로 통과시킨 기존 ID. 클라이언트는 이걸 sessionStorage에 보관.
 *
 * @param reply 챗봇 답변 텍스트
 * @param welfares 추천 복지 목록 (챗봇 반환 순서 유지)
 * @param conversationId 대화 컨텍스트 ID (UUID v4)
 */
public record ChatResponse(
        String reply,
        List<WelfareSummaryDto> welfares,
        String conversationId
) {
}
// 이 record의 역할: 챗봇 응답을 우리 자체 API 형식으로 가공한 페이로드.
// welfares가 검색 API와 동일 형식이라 프론트 컴포넌트 통일 가능.
