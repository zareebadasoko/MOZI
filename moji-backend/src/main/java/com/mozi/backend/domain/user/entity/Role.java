package com.mozi.backend.domain.user.entity;

/**
 * 사용자 권한 등급.
 *
 * Spring Security의 권한 체계와 연동될 enum. MVP 시연에서는 USER만 사용하지만,
 * 향후 관리자 대시보드나 백오피스 도입 시 ADMIN을 활용할 수 있도록 미리 정의.
 * @Enumerated(EnumType.STRING) 으로 매핑되어 DB에는 "USER" / "ADMIN" 문자열 저장.
 */
public enum Role {
    USER,
    ADMIN
}
// 이 enum의 역할: 사용자 권한 표현.
// EnumType.ORDINAL이 아닌 STRING으로 저장하는 이유는 새 권한이 중간 순서로 추가되어도
// 기존 row의 의미가 깨지지 않게 하기 위함이다.
