package com.mozi.backend.domain.user.dto;

/**
 * 로그아웃 응답 DTO.
 *
 * 단일 boolean 플래그로 충분하지만 일관된 ApiResponse 페이로드 구조를
 * 유지하기 위해 record로 감싼다. true 외의 값을 반환할 일은 사실상 없으나
 * 향후 "어떤 기기에서 종료됐는지" 같은 메타 정보 확장 여지를 남긴다.
 *
 * @param loggedOut 항상 true (로그아웃 정상 처리)
 */
public record LogoutResponse(boolean loggedOut) {
}
// 이 record의 역할: logout 응답 페이로드를 일관된 객체 형태로 유지.
