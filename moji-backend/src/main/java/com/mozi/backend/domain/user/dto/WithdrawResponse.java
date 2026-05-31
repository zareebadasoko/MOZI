package com.mozi.backend.domain.user.dto;

/**
 * 회원 탈퇴 응답 DTO.
 *
 * Hard Delete 정책상 cascade로 UserProfile/Bookmark/RefreshToken까지 모두
 * 삭제된 후 호출자가 받는 마지막 메시지. 단일 플래그지만 일관된 페이로드 구조를 유지.
 *
 * @param withdrawn 항상 true (정상 탈퇴)
 */
public record WithdrawResponse(boolean withdrawn) {
}
// 이 record의 역할: 회원 탈퇴 성공 응답 페이로드.
