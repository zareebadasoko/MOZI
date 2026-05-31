package com.mozi.backend.global.security;

import com.mozi.backend.domain.user.entity.Role;

/**
 * 인증된 요청의 SecurityContext에 담기는 principal 타입.
 *
 * JwtAuthenticationFilter가 토큰을 파싱한 뒤 이 record를
 * UsernamePasswordAuthenticationToken.principal로 넣는다.
 * 컨트롤러는 @AuthenticationPrincipal AuthenticatedUser u 형태로 받아
 * u.userId() 만 추출해서 비즈니스 레이어에 전달한다.
 *
 * @param userId 인증된 사용자의 PK
 * @param role 권한 enum (메서드별 인가 판단 시 사용 예정)
 */
public record AuthenticatedUser(Long userId, Role role) {
}
// 이 record의 역할: SecurityContext의 principal 타입을 명시적 record로 고정한다.
// String이나 Map 같은 느슨한 타입 대신 컴파일 타임에 잘못된 사용을 차단한다.
