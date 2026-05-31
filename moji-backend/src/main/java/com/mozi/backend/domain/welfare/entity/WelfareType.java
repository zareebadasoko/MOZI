package com.mozi.backend.domain.welfare.entity;

/**
 * 복지 정보의 출처를 구분하는 enum.
 *
 * 4가지 크롤링 출처(복지로 중앙/지자체/민간 + 서울복지포털)별로 데이터 스키마가
 * 달라 JPA JOINED 상속 매핑의 Discriminator 값으로 사용한다. WelfareCommon 부모
 * 테이블의 welfare_type 컬럼에 STRING으로 기록되며, 각 자식 엔티티는
 * @DiscriminatorValue로 자신의 enum 이름을 선언한다.
 */
public enum WelfareType {
    CENTRAL,
    LOCAL,
    PRIVATE,
    SEOUL
}
// 이 enum의 역할: 복지 출처 식별 + JPA Discriminator 값.
// EnumType.STRING으로 매핑되며 DB에는 "CENTRAL"/"LOCAL"/"PRIVATE"/"SEOUL" 문자열로 저장.
