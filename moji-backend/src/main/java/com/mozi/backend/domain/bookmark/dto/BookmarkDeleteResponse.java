package com.mozi.backend.domain.bookmark.dto;

/**
 * DELETE /api/bookmarks/{welfareId} 응답 DTO.
 *
 * 단일 boolean 플래그지만 ApiResponse 페이로드 구조 일관성을 위해 record로 감싼다.
 * 항상 true이며 false 케이스는 도달 불가능 — 삭제 실패 시에는 BookmarkNotFoundException 또는
 * WelfareNotFoundException으로 빠지므로 본 응답은 항상 deleted=true.
 *
 * @param deleted 항상 true (정상 삭제)
 */
public record BookmarkDeleteResponse(boolean deleted) {
}
// 이 record의 역할: 북마크 삭제 성공 응답.
