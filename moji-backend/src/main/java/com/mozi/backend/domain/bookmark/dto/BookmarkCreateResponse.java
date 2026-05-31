package com.mozi.backend.domain.bookmark.dto;

/**
 * POST /api/bookmarks/{welfareId} 응답 DTO.
 *
 * 신규 생성/idempotent(이미 존재) 두 케이스 모두 동일한 구조로 응답한다 — 클라이언트 입장에서
 * "이미 있었어요" 같은 별도 안내가 필요 없다는 UX 결정에 따른 것. 둘 다 HTTP 200 + bookmarkId 반환.
 *
 * @param bookmarkId 신규 또는 기존 북마크의 PK
 */
public record BookmarkCreateResponse(Long bookmarkId) {
}
// 이 record의 역할: 북마크 추가 응답 페이로드.
// idempotent 동작상 신규/기존 ID가 동일 형식으로 노출돼 클라이언트가 분기 처리할 필요가 없다.
