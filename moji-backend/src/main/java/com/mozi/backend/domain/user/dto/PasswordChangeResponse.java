package com.mozi.backend.domain.user.dto;

/**
 * 비밀번호 변경 응답 DTO.
 *
 * 단일 boolean 플래그로 충분하지만 ApiResponse 페이로드 구조를 일관되게 유지하기 위해 record로 감싼다.
 *
 * @param changed 항상 true (정상 변경)
 */
public record PasswordChangeResponse(boolean changed) {
}
// 이 record의 역할: 비밀번호 변경 성공 응답 페이로드.
