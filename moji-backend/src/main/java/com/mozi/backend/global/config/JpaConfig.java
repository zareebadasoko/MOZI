package com.mozi.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing을 활성화하는 설정 클래스.
 *
 * BaseEntity의 @CreatedDate / @LastModifiedDate가 자동으로 채워지려면
 * @EnableJpaAuditing이 어딘가에 한 번은 선언돼야 한다. BackendApplication에
 * 직접 붙이지 않고 별도 클래스로 분리한 이유는 향후 Auditor(누가 수정했는지)
 * 추가, JPA 관련 다른 설정을 모을 확장 지점을 두기 위함.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
// 이 클래스의 역할: JPA Auditing의 단일 스위치.
// 이 어노테이션이 빠지면 BaseEntity 상속 엔티티의 createdAt/updatedAt이 null로 남음.
